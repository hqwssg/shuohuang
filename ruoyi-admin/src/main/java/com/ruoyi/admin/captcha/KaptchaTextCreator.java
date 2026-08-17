package com.ruoyi.admin.captcha;

import com.google.code.kaptcha.text.impl.DefaultTextCreator;
import java.util.Random;

public class KaptchaTextCreator extends DefaultTextCreator
{
    private static final String[] NUMBERS = "0,1,2,3,4,5,6,7,8,9,10".split(",");

    @Override
    public String getText()
    {
        int result;
        Random random = new Random();
        int x = random.nextInt(10);
        int y = random.nextInt(10);
        StringBuilder text = new StringBuilder();
        int operand = random.nextInt(3);
        if (operand == 0)
        {
            result = x * y;
            text.append(NUMBERS[x]).append("*").append(NUMBERS[y]);
        }
        else if (operand == 1)
        {
            if (x != 0 && y % x == 0)
            {
                result = y / x;
                text.append(NUMBERS[y]).append("/").append(NUMBERS[x]);
            }
            else
            {
                result = x + y;
                text.append(NUMBERS[x]).append("+").append(NUMBERS[y]);
            }
        }
        else if (x >= y)
        {
            result = x - y;
            text.append(NUMBERS[x]).append("-").append(NUMBERS[y]);
        }
        else
        {
            result = y - x;
            text.append(NUMBERS[y]).append("-").append(NUMBERS[x]);
        }
        text.append("=?@").append(result);
        return text.toString();
    }
}
