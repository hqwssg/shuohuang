#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
蒸汽热力学计算模块
使用IAPWS热力学公式计算饱和蒸汽和过热蒸汽的物理参数
"""

import sys
import json
import math


def calculate_saturation_properties(T):
    """
    计算给定温度下的饱和蒸汽性质（压力和焓值）
    使用IAPWS-IF97公式

    参数:
        T: 温度，单位摄氏度（°C）

    返回:
        dict: 包含 'pressure' (MPa) 和 'enthalpy' (kJ/kg)
    """
    try:
        # 尝试导入iapws库
        try:
            from iapws import IAPWS97
            # IAPWS97使用绝对温度(K)和压力(MPa)
            # 创建饱和蒸汽对象，传入温度
            sat_steam = IAPWS97(T=T + 273.15, x=1)  # x=1表示饱和蒸汽
            pressure = sat_steam.P  # MPa
            enthalpy = sat_steam.h  # kJ/kg
            return {
                'success': True,
                'pressure': pressure,
                'enthalpy': enthalpy,
                'unit': 'MPa for pressure, kJ/kg for enthalpy'
            }
        except ImportError:
            # 如果iapws库不可用，使用公式计算
            return calculate_saturation_formula(T)

    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


def calculate_saturation_formula(T):
    """
    使用IAPWS-IF97区域2公式计算饱和蒸汽性质
    当iapws库不可用时使用此方法

    参数:
        T: 温度，单位摄氏度（°C）

    返回:
        dict: 包含 'pressure' (MPa) 和 'enthalpy' (kJ/kg)
    """
    try:
        # 将温度转换为开尔文
        T_kelvin = T + 273.15

        # 使用饱和蒸汽压力公式 (IAPWS-IF97)
        # n1到n5是公式系数
        n1 = 0.11670521452767e+04
        n2 = -0.72421316703206e+06
        n3 = -0.17070748946286e+01
        n4 = 0.12098247947526e+03
        n5 = -0.32985491135024e+05

        # 计算压力 (MPa)
        sigma = T + n3
        A = (sigma + n4) * sigma + n5
        B = n2 + (n4 + 2 * T) * T
        pressure = (2 * B / (-A + math.sqrt(A * A - 4 * B * sigma))) ** 4 * 1e-6

        # 计算焓值 (kJ/kg)
        # 使用简化公式计算饱和蒸汽焓
        # h = 2500 + 2.0 * T (简化公式，适用于近似计算)
        # 使用更精确的公式
        # 饱和蒸汽焓的近似计算
        h_sat_steam = 2501.0 + 2.080 * T + 0.00189 * T * T

        return {
            'success': True,
            'pressure': pressure,
            'enthalpy': h_sat_steam,
            'note': 'Calculated using approximate formula'
        }
    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


def calculate_superheated_properties(T, P):
    """
    计算过热蒸汽的性质（焓值）
    使用IAPWS-IF97公式

    参数:
        T: 温度，单位摄氏度（°C）
        P: 压力，单位MPa

    返回:
        dict: 包含 'enthalpy' (kJ/kg) 和验证信息
    """
    try:
        # 根据输入压力计算饱和温度
        T_sat = calculate_saturation_temperature(P)
        
        # 检查温度是否满足过热水蒸汽的形成条件
        # 过热蒸汽要求输入温度 > 饱和温度
        if T <= T_sat:
            return {
                'success': False,
                'error': f'输入的温度和压力不满足过热水蒸汽的形成条件！压力{P}MPa下的饱和温度约为{T_sat:.2f}°C，输入温度{T}°C应高于饱和温度。',
                'saturation_temperature': T_sat,
                'input_temperature': T
            }

        # 计算给定温度和压力下的过热蒸汽焓值
        try:
            from iapws import IAPWS97
            # 创建过热蒸汽对象
            steam = IAPWS97(T=T + 273.15, P=P)
            enthalpy = steam.h  # kJ/kg
            return {
                'success': True,
                'enthalpy': enthalpy,
                'saturation_temperature': T_sat,
                'input_temperature': T
            }
        except ImportError:
            # 使用近似公式计算过热蒸汽焓
            return calculate_superheated_formula(T, P, T_sat)

    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


def calculate_saturation_temperature(P):
    """
    根据压力计算饱和温度（°C）
    当iapws库不可用时使用近似公式
    """
    try:
        from iapws import IAPWS97
        sat_steam = IAPWS97(P=P, x=1)
        return sat_steam.T - 273.15
    except ImportError:
        pass
    
    # 使用安托万方程近似计算饱和温度
    # ln(P*10^6/101325) = A - B/(T + C)
    # P单位转换为Pa
    P_pa = P * 1e6
    A = 8.07131
    B = 1730.63
    C = 233.426
    
    T = 100.0
    for _ in range(20):
        f = math.log(P_pa / 101325) - (A - B / (T + C))
        f_prime = B / (T + C)**2
        T -= f / f_prime
        if abs(f) < 1e-6:
            break
    return T


def calculate_superheated_formula(T, P, saturation_pressure):
    """
    使用近似公式计算过热蒸汽焓值
    当iapws库不可用时使用此方法

    参数:
        T: 温度，单位摄氏度（°C）
        P: 压力，单位MPa
        saturation_pressure: 饱和压力，单位MPa

    返回:
        dict: 包含 'enthalpy' (kJ/kg)
    """
    try:
        # 根据压力计算饱和温度
        T_sat = calculate_saturation_temperature(P)
        
        # 获取饱和蒸汽焓（使用饱和温度）
        sat_props = calculate_saturation_properties(T_sat)
        h_sat_steam = sat_props['enthalpy']

        # 过热蒸汽焓的近似计算：饱和蒸汽焓 + 过热部分焓增
        # 过热部分：温度高于饱和温度的部分，比热容约2.1 kJ/(kg·K)
        delta_T = T - T_sat
        h_superheated = h_sat_steam + 2.1 * delta_T

        return {
            'success': True,
            'enthalpy': h_superheated,
            'saturation_pressure': saturation_pressure,
            'input_pressure': P,
            'note': 'Calculated using approximate formula'
        }
    except Exception as e:
        return {
            'success': False,
            'error': str(e)
        }


def main():
    """
    主函数，处理命令行参数
    """
    if len(sys.argv) < 2:
        print(json.dumps({'success': False, 'error': '缺少参数'}))
        sys.exit(1)

    command = sys.argv[1]

    try:
        if command == 'saturation':
            # 计算饱和蒸汽性质
            if len(sys.argv) < 3:
                print(json.dumps({'success': False, 'error': '缺少温度参数'}))
                sys.exit(1)
            T = float(sys.argv[2])
            result = calculate_saturation_properties(T)
            print(json.dumps(result, ensure_ascii=False))

        elif command == 'superheated':
            # 计算过热蒸汽性质
            if len(sys.argv) < 4:
                print(json.dumps({'success': False, 'error': '缺少温度或压力参数'}))
                sys.exit(1)
            T = float(sys.argv[2])
            P = float(sys.argv[3])
            result = calculate_superheated_properties(T, P)
            print(json.dumps(result, ensure_ascii=False))

        else:
            print(json.dumps({'success': False, 'error': f'未知命令: {command}'}))
            sys.exit(1)

    except Exception as e:
        print(json.dumps({'success': False, 'error': str(e)}, ensure_ascii=False))
        sys.exit(1)


if __name__ == '__main__':
    main()
