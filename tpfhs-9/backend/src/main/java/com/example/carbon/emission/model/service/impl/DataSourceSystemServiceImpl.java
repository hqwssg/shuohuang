package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.DataSourceSystem;
import com.example.carbon.emission.model.repository.DataSourceSystemRepository;
import com.example.carbon.emission.model.service.DataSourceSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 数据来源系统服务实现类
 * 实现数据来源系统的业务逻辑
 */
@Service
@Transactional
public class DataSourceSystemServiceImpl implements DataSourceSystemService {
    
    @Autowired
    private DataSourceSystemRepository dataSourceSystemRepository;
    
    /**
     * 常用汉字拼音映射表，用于生成拼音首字母编码
     */
    private static final Map<Character, String> PINYIN_MAP = new HashMap<>();
    
    // 初始化拼音映射，包含常用汉字
    static {
        String[] pinyins = {
            "a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a",
            "a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a",
            "a","ai","ai","ai","an","an","an","an","ang","ang","ao","ao","ao","ba","ba","ba",
            "ba","ba","ba","ba","bai","bai","bai","bai","bai","bai","bai","ban","ban","ban",
            "ban","ban","bang","bang","bang","bao","bao","bao","bao","bao","bao","bei","bei",
            "bei","bei","bei","bei","bei","ben","ben","ben","ben","beng","beng","bi","bi","bi",
            "bi","bi","bi","bi","bi","bian","bian","bian","bian","bian","bian","bian","biao",
            "biao","biao","biao","bie","bie","bin","bin","bing","bing","bo","bo","bo","bo","bo",
            "bu","bu","ca","ca","cai","cai","cai","cai","can","can","can","can","cang","cang",
            "cang","cao","cao","ce","ce","ce","cen","ceng","cha","cha","cha","cha","cha","chai",
            "chai","chai","chan","chan","chan","chan","chang","chang","chang","chang","chang",
            "chao","chao","chao","chao","che","che","chen","chen","chen","cheng","cheng","cheng",
            "cheng","chi","chi","chi","chi","chong","chong","chou","chou","chou","chu","chu",
            "chu","chu","chuai","chuan","chuan","chuang","chuang","chui","chui","chun","chun",
            "chuo","ci","ci","ci","ci","cong","cou","cu","cu","cuan","cuan","cui","cui","cui",
            "cun","cuo","da","da","da","da","da","da","dai","dai","dai","dai","dan","dan","dan",
            "dan","dan","dang","dao","dao","dao","dao","dao","de","de","dei","deng","di","di",
            "di","di","di","di","dian","dian","dian","dian","dian","diao","diao","diao","diao",
            "die","die","ding","ding","ding","diu","dong","dou","du","du","du","duan","duan",
            "dui","dui","dun","dun","duo","duo","e","e","e","e","e","e","e","e","e","e","e","e",
            "e","e","e","e","en","er","fa","fa","fa","fan","fan","fan","fan","fang","fang","fang",
            "fei","fei","fen","fen","feng","fo","fou","fu","fu","fu","fu","fu","fu","fu","fu",
            "ga","gai","gai","gan","gan","gan","gang","gang","gang","gao","gao","gao","ge","ge",
            "ge","ge","gei","gen","geng","gong","gong","gong","gong","gou","gou","gu","gu","gu",
            "gua","guai","guan","guan","guang","gui","gui","gui","gui","gun","guo","ha","ha",
            "hai","han","han","han","hang","hao","hao","he","he","hei","hen","heng","hong","hou",
            "hu","hua","huai","huan","huang","hui","hui","hui","hun","huo","huo","ji","ji","ji",
            "ji","ji","jia","jia","jia","jian","jian","jian","jiang","jiang","jiao","jiao","jiao",
            "jie","jie","jin","jin","jin","jing","jing","jiong","jiu","ju","juan","jue","jun","ka",
            "kai","kai","kan","kan","kang","kao","ke","ke","ke","ke","ken","keng","kong","kou",
            "ku","kua","kuai","kuan","kuang","kui","kun","kuo","la","la","lai","lan","lan","lang",
            "lang","lao","le","lei","lei","leng","li","li","li","li","lia","lian","lian","liang",
            "liang","liao","liao","lie","lin","lin","ling","liu","long","lou","lu","lu","lu","lv",
            "luan","lue","lve","lun","luo","ma","ma","ma","mai","mai","man","mang","mao","mao",
            "me","mei","mei","men","meng","mi","mi","mian","mian","miao","mie","min","ming","miu",
            "mo","mo","mou","mu","na","na","nai","nan","nang","nao","ne","nei","nen","neng","ni",
            "nian","niang","niao","nie","nin","ning","niu","nong","nu","nv","nuan","nve","nuo",
            "o","ou","pa","pai","pan","pang","pao","pei","pen","peng","pi","pian","piao","pie",
            "pin","ping","po","pou","pu","qi","qi","qi","qia","qian","qian","qiang","qiao","qie",
            "qin","qing","qing","qiong","qiu","qu","qu","quan","que","qun","ran","rang","rao",
            "re","ren","reng","ri","rong","rou","ru","ruan","rui","run","ruo","sa","sai","san",
            "sang","sao","se","sen","seng","sha","sha","shai","shan","shan","shang","shao","shao",
            "she","she","shen","shen","sheng","shi","shi","shou","shu","shu","shua","shuai","shuan",
            "shuang","shui","shun","shuo","si","si","song","sou","su","su","suan","sui","sui",
            "sun","suo","ta","ta","tai","tan","tan","tang","tang","tao","te","teng","ti","ti",
            "tian","tiao","tie","ting","tong","tong","tou","tu","tuan","tui","tun","tuo","wa",
            "wai","wan","wan","wang","wei","wei","wen","weng","wo","wu","xi","xi","xia","xia",
            "xian","xiang","xiao","xiao","xie","xie","xin","xing","xing","xiong","xiu","xu",
            "xu","xuan","xue","xue","xun","ya","yan","yan","yan","yang","yang","yao","ye","yi",
            "yin","yin","ying","ying","yo","yong","you","yu","yu","yu","yuan","yue","yun","za",
            "zai","zan","zang","zao","ze","zei","zen","zeng","zha","zhai","zhan","zhang","zhang",
            "zhao","zhe","zhe","zhen","zheng","zheng","zhi","zhong","zhou","zhu","zhua","zhuai",
            "zhuan","zhuang","zhui","zhun","zhuo","zi","zi","zong","zou","zu","zuan","zui","zun",
            "zuo"
        };
        
        for (int i = 0; i < pinyins.length && i < 20902; i++) {
            char c = (char) (0x4E00 + i);
            PINYIN_MAP.put(c, pinyins[i]);
        }
        
        addCommonChar("啊", "a");
        addCommonChar("阿", "a");
        addCommonChar("爱", "ai");
        addCommonChar("安", "an");
        addCommonChar("暗", "an");
        addCommonChar("昂", "ang");
        addCommonChar("奥", "ao");
        addCommonChar("吧", "ba");
        addCommonChar("把", "ba");
        addCommonChar("八", "ba");
        addCommonChar("巴", "ba");
        addCommonChar("白", "bai");
        addCommonChar("百", "bai");
        addCommonChar("办", "ban");
        addCommonChar("半", "ban");
        addCommonChar("邦", "bang");
        addCommonChar("宝", "bao");
        addCommonChar("保", "bao");
        addCommonChar("报", "bao");
        addCommonChar("北", "bei");
        addCommonChar("被", "bei");
        addCommonChar("本", "ben");
        addCommonChar("比", "bi");
        addCommonChar("笔", "bi");
        addCommonChar("必", "bi");
        addCommonChar("毕", "bi");
        addCommonChar("闭", "bi");
        addCommonChar("编", "bian");
        addCommonChar("边", "bian");
        addCommonChar("变", "bian");
        addCommonChar("标", "biao");
        addCommonChar("表", "biao");
        addCommonChar("别", "bie");
        addCommonChar("病", "bing");
        addCommonChar("波", "bo");
        addCommonChar("博", "bo");
        addCommonChar("不", "bu");
        addCommonChar("布", "bu");
        addCommonChar("才", "cai");
        addCommonChar("财", "cai");
        addCommonChar("彩", "cai");
        addCommonChar("菜", "cai");
        addCommonChar("参", "can");
        addCommonChar("操", "cao");
        addCommonChar("草", "cao");
        addCommonChar("测", "ce");
        addCommonChar("层", "ceng");
        addCommonChar("曾", "ceng");
        addCommonChar("查", "cha");
        addCommonChar("产", "chan");
        addCommonChar("长", "chang");
        addCommonChar("场", "chang");
        addCommonChar("常", "chang");
        addCommonChar("超", "chao");
        addCommonChar("车", "che");
        addCommonChar("称", "cheng");
        addCommonChar("成", "cheng");
        addCommonChar("城", "cheng");
        addCommonChar("程", "cheng");
        addCommonChar("吃", "chi");
        addCommonChar("持", "chi");
        addCommonChar("充", "chong");
        addCommonChar("出", "chu");
        addCommonChar("除", "chu");
        addCommonChar("传", "chuan");
        addCommonChar("创", "chuang");
        addCommonChar("春", "chun");
        addCommonChar("词", "ci");
        addCommonChar("此", "ci");
        addCommonChar("次", "ci");
        addCommonChar("从", "cong");
        addCommonChar("村", "cun");
        addCommonChar("错", "cuo");
        addCommonChar("打", "da");
        addCommonChar("大", "da");
        addCommonChar("代", "dai");
        addCommonChar("单", "dan");
        addCommonChar("当", "dang");
        addCommonChar("档", "dang");
        addCommonChar("到", "dao");
        addCommonChar("道", "dao");
        addCommonChar("得", "de");
        addCommonChar("的", "de");
        addCommonChar("等", "deng");
        addCommonChar("低", "di");
        addCommonChar("底", "di");
        addCommonChar("地", "di");
        addCommonChar("点", "dian");
        addCommonChar("电", "dian");
        addCommonChar("店", "dian");
        addCommonChar("调", "diao");
        addCommonChar("顶", "ding");
        addCommonChar("定", "ding");
        addCommonChar("丢", "diu");
        addCommonChar("东", "dong");
        addCommonChar("动", "dong");
        addCommonChar("读", "du");
        addCommonChar("度", "du");
        addCommonChar("短", "duan");
        addCommonChar("段", "duan");
        addCommonChar("对", "dui");
        addCommonChar("多", "duo");
        addCommonChar("儿", "er");
        addCommonChar("而", "er");
        addCommonChar("发", "fa");
        addCommonChar("法", "fa");
        addCommonChar("翻", "fan");
        addCommonChar("方", "fang");
        addCommonChar("放", "fang");
        addCommonChar("非", "fei");
        addCommonChar("飞", "fei");
        addCommonChar("分", "fen");
        addCommonChar("份", "fen");
        addCommonChar("风", "feng");
        addCommonChar("服", "fu");
        addCommonChar("复", "fu");
        addCommonChar("该", "gai");
        addCommonChar("改", "gai");
        addCommonChar("感", "gan");
        addCommonChar("刚", "gang");
        addCommonChar("高", "gao");
        addCommonChar("告", "gao");
        addCommonChar("个", "ge");
        addCommonChar("给", "gei");
        addCommonChar("跟", "gen");
        addCommonChar("更", "geng");
        addCommonChar("工", "gong");
        addCommonChar("公", "gong");
        addCommonChar("共", "gong");
        addCommonChar("够", "gou");
        addCommonChar("古", "gu");
        addCommonChar("故", "gu");
        addCommonChar("关", "guan");
        addCommonChar("管", "guan");
        addCommonChar("光", "guang");
        addCommonChar("广", "guang");
        addCommonChar("贵", "gui");
        addCommonChar("国", "guo");
        addCommonChar("过", "guo");
        addCommonChar("还", "hai");
        addCommonChar("孩", "hai");
        addCommonChar("海", "hai");
        addCommonChar("害", "hai");
        addCommonChar("汉", "han");
        addCommonChar("号", "hao");
        addCommonChar("好", "hao");
        addCommonChar("喝", "he");
        addCommonChar("合", "he");
        addCommonChar("何", "he");
        addCommonChar("和", "he");
        addCommonChar("黑", "hei");
        addCommonChar("很", "hen");
        addCommonChar("红", "hong");
        addCommonChar("后", "hou");
        addCommonChar("呼", "hu");
        addCommonChar("湖", "hu");
        addCommonChar("护", "hu");
        addCommonChar("花", "hua");
        addCommonChar("化", "hua");
        addCommonChar("划", "hua");
        addCommonChar("画", "hua");
        addCommonChar("话", "hua");
        addCommonChar("环", "huan");
        addCommonChar("换", "huan");
        addCommonChar("黄", "huang");
        addCommonChar("回", "hui");
        addCommonChar("会", "hui");
        addCommonChar("婚", "hun");
        addCommonChar("活", "huo");
        addCommonChar("火", "huo");
        addCommonChar("或", "huo");
        addCommonChar("机", "ji");
        addCommonChar("基", "ji");
        addCommonChar("级", "ji");
        addCommonChar("即", "ji");
        addCommonChar("济", "ji");
        addCommonChar("记", "ji");
        addCommonChar("继", "ji");
        addCommonChar("纪", "ji");
        addCommonChar("技", "ji");
        addCommonChar("季", "ji");
        addCommonChar("加", "jia");
        addCommonChar("家", "jia");
        addCommonChar("价", "jia");
        addCommonChar("架", "jia");
        addCommonChar("见", "jian");
        addCommonChar("件", "jian");
        addCommonChar("建", "jian");
        addCommonChar("江", "jiang");
        addCommonChar("讲", "jiang");
        addCommonChar("交", "jiao");
        addCommonChar("脚", "jiao");
        addCommonChar("叫", "jiao");
        addCommonChar("街", "jie");
        addCommonChar("节", "jie");
        addCommonChar("解", "jie");
        addCommonChar("姐", "jie");
        addCommonChar("今", "jin");
        addCommonChar("金", "jin");
        addCommonChar("仅", "jin");
        addCommonChar("紧", "jin");
        addCommonChar("尽", "jin");
        addCommonChar("进", "jin");
        addCommonChar("近", "jin");
        addCommonChar("京", "jing");
        addCommonChar("经", "jing");
        addCommonChar("精", "jing");
        addCommonChar("井", "jing");
        addCommonChar("静", "jing");
        addCommonChar("九", "jiu");
        addCommonChar("酒", "jiu");
        addCommonChar("久", "jiu");
        addCommonChar("旧", "jiu");
        addCommonChar("就", "jiu");
        addCommonChar("举", "ju");
        addCommonChar("据", "ju");
        addCommonChar("具", "ju");
        addCommonChar("剧", "ju");
        addCommonChar("觉", "jue");
        addCommonChar("开", "kai");
        addCommonChar("看", "kan");
        addCommonChar("考", "kao");
        addCommonChar("靠", "kao");
        addCommonChar("可", "ke");
        addCommonChar("克", "ke");
        addCommonChar("课", "ke");
        addCommonChar("刻", "ke");
        addCommonChar("客", "ke");
        addCommonChar("空", "kong");
        addCommonChar("口", "kou");
        addCommonChar("哭", "ku");
        addCommonChar("苦", "ku");
        addCommonChar("快", "kuai");
        addCommonChar("块", "kuai");
        addCommonChar("况", "kuang");
        addCommonChar("来", "lai");
        addCommonChar("蓝", "lan");
        addCommonChar("浪", "lang");
        addCommonChar("老", "lao");
        addCommonChar("乐", "le");
        addCommonChar("累", "lei");
        addCommonChar("冷", "leng");
        addCommonChar("离", "li");
        addCommonChar("里", "li");
        addCommonChar("理", "li");
        addCommonChar("礼", "li");
        addCommonChar("力", "li");
        addCommonChar("历", "li");
        addCommonChar("立", "li");
        addCommonChar("利", "li");
        addCommonChar("连", "lian");
        addCommonChar("脸", "lian");
        addCommonChar("练", "lian");
        addCommonChar("凉", "liang");
        addCommonChar("两", "liang");
        addCommonChar("亮", "liang");
        addCommonChar("量", "liang");
        addCommonChar("列", "lie");
        addCommonChar("林", "lin");
        addCommonChar("零", "ling");
        addCommonChar("领", "ling");
        addCommonChar("另", "ling");
        addCommonChar("留", "liu");
        addCommonChar("流", "liu");
        addCommonChar("六", "liu");
        addCommonChar("龙", "long");
        addCommonChar("楼", "lou");
        addCommonChar("露", "lu");
        addCommonChar("路", "lu");
        addCommonChar("陆", "lu");
        addCommonChar("录", "lu");
        addCommonChar("旅", "lv");
        addCommonChar("绿", "lv");
        addCommonChar("乱", "luan");
        addCommonChar("略", "lve");
        addCommonChar("码", "ma");
        addCommonChar("马", "ma");
        addCommonChar("吗", "ma");
        addCommonChar("买", "mai");
        addCommonChar("卖", "mai");
        addCommonChar("慢", "man");
        addCommonChar("满", "man");
        addCommonChar("忙", "mang");
        addCommonChar("毛", "mao");
        addCommonChar("没", "mei");
        addCommonChar("每", "mei");
        addCommonChar("美", "mei");
        addCommonChar("妹", "mei");
        addCommonChar("门", "men");
        addCommonChar("们", "men");
        addCommonChar("迷", "mi");
        addCommonChar("米", "mi");
        addCommonChar("面", "mian");
        addCommonChar("秒", "miao");
        addCommonChar("民", "min");
        addCommonChar("明", "ming");
        addCommonChar("名", "ming");
        addCommonChar("命", "ming");
        addCommonChar("摸", "mo");
        addCommonChar("末", "mo");
        addCommonChar("母", "mu");
        addCommonChar("木", "mu");
        addCommonChar("目", "mu");
        addCommonChar("拿", "na");
        addCommonChar("哪", "na");
        addCommonChar("那", "na");
        addCommonChar("奶", "nai");
        addCommonChar("男", "nan");
        addCommonChar("南", "nan");
        addCommonChar("呢", "ne");
        addCommonChar("能", "neng");
        addCommonChar("你", "ni");
        addCommonChar("年", "nian");
        addCommonChar("念", "nian");
        addCommonChar("鸟", "niao");
        addCommonChar("您", "nin");
        addCommonChar("牛", "niu");
        addCommonChar("农", "nong");
        addCommonChar("女", "nv");
        addCommonChar("暖", "nuan");
        addCommonChar("怕", "pa");
        addCommonChar("排", "pai");
        addCommonChar("派", "pai");
        addCommonChar("盘", "pan");
        addCommonChar("跑", "pao");
        addCommonChar("配", "pei");
        addCommonChar("朋", "peng");
        addCommonChar("皮", "pi");
        addCommonChar("片", "pian");
        addCommonChar("票", "piao");
        addCommonChar("漂", "piao");
        addCommonChar("品", "pin");
        addCommonChar("平", "ping");
        addCommonChar("评", "ping");
        addCommonChar("破", "po");
        addCommonChar("普", "pu");
        addCommonChar("七", "qi");
        addCommonChar("期", "qi");
        addCommonChar("其", "qi");
        addCommonChar("奇", "qi");
        addCommonChar("骑", "qi");
        addCommonChar("起", "qi");
        addCommonChar("气", "qi");
        addCommonChar("汽", "qi");
        addCommonChar("器", "qi");
        addCommonChar("千", "qian");
        addCommonChar("强", "qiang");
        addCommonChar("墙", "qiang");
        addCommonChar("桥", "qiao");
        addCommonChar("巧", "qiao");
        addCommonChar("青", "qing");
        addCommonChar("轻", "qing");
        addCommonChar("清", "qing");
        addCommonChar("情", "qing");
        addCommonChar("请", "qing");
        addCommonChar("秋", "qiu");
        addCommonChar("球", "qiu");
        addCommonChar("区", "qu");
        addCommonChar("去", "qu");
        addCommonChar("全", "quan");
        addCommonChar("却", "que");
        addCommonChar("群", "qun");
        addCommonChar("然", "ran");
        addCommonChar("让", "rang");
        addCommonChar("热", "re");
        addCommonChar("人", "ren");
        addCommonChar("认", "ren");
        addCommonChar("日", "ri");
        addCommonChar("容", "rong");
        addCommonChar("肉", "rou");
        addCommonChar("如", "ru");
        addCommonChar("软", "ruan");
        addCommonChar("若", "ruo");
        addCommonChar("三", "san");
        addCommonChar("色", "se");
        addCommonChar("山", "shan");
        addCommonChar("上", "shang");
        addCommonChar("少", "shao");
        addCommonChar("社", "she");
        addCommonChar("身", "shen");
        addCommonChar("深", "shen");
        addCommonChar("什", "shen");
        addCommonChar("生", "sheng");
        addCommonChar("声", "sheng");
        addCommonChar("省", "sheng");
        addCommonChar("胜", "sheng");
        addCommonChar("失", "shi");
        addCommonChar("师", "shi");
        addCommonChar("十", "shi");
        addCommonChar("时", "shi");
        addCommonChar("实", "shi");
        addCommonChar("食", "shi");
        addCommonChar("始", "shi");
        addCommonChar("世", "shi");
        addCommonChar("市", "shi");
        addCommonChar("事", "shi");
        addCommonChar("室", "shi");
        addCommonChar("是", "shi");
        addCommonChar("适", "shi");
        addCommonChar("收", "shou");
        addCommonChar("手", "shou");
        addCommonChar("受", "shou");
        addCommonChar("书", "shu");
        addCommonChar("树", "shu");
        addCommonChar("双", "shuang");
        addCommonChar("水", "shui");
        addCommonChar("睡", "shui");
        addCommonChar("顺", "shun");
        addCommonChar("思", "si");
        addCommonChar("死", "si");
        addCommonChar("四", "si");
        addCommonChar("松", "song");
        addCommonChar("诉", "su");
        addCommonChar("速", "su");
        addCommonChar("算", "suan");
        addCommonChar("虽", "sui");
        addCommonChar("岁", "sui");
        addCommonChar("孙", "sun");
        addCommonChar("所", "suo");
        addCommonChar("台", "tai");
        addCommonChar("太", "tai");
        addCommonChar("态", "tai");
        addCommonChar("谈", "tan");
        addCommonChar("汤", "tang");
        addCommonChar("糖", "tang");
        addCommonChar("特", "te");
        addCommonChar("疼", "teng");
        addCommonChar("提", "ti");
        addCommonChar("体", "ti");
        addCommonChar("天", "tian");
        addCommonChar("田", "tian");
        addCommonChar("条", "tiao");
        addCommonChar("铁", "tie");
        addCommonChar("听", "ting");
        addCommonChar("停", "ting");
        addCommonChar("通", "tong");
        addCommonChar("同", "tong");
        addCommonChar("头", "tou");
        addCommonChar("图", "tu");
        addCommonChar("土", "tu");
        addCommonChar("团", "tuan");
        addCommonChar("推", "tui");
        addCommonChar("腿", "tui");
        addCommonChar("外", "wai");
        addCommonChar("完", "wan");
        addCommonChar("玩", "wan");
        addCommonChar("晚", "wan");
        addCommonChar("万", "wan");
        addCommonChar("王", "wang");
        addCommonChar("往", "wang");
        addCommonChar("网", "wang");
        addCommonChar("忘", "wang");
        addCommonChar("危", "wei");
        addCommonChar("为", "wei");
        addCommonChar("位", "wei");
        addCommonChar("文", "wen");
        addCommonChar("问", "wen");
        addCommonChar("我", "wo");
        addCommonChar("屋", "wu");
        addCommonChar("五", "wu");
        addCommonChar("午", "wu");
        addCommonChar("物", "wu");
        addCommonChar("务", "wu");
        addCommonChar("西", "xi");
        addCommonChar("息", "xi");
        addCommonChar("希", "xi");
        addCommonChar("习", "xi");
        addCommonChar("洗", "xi");
        addCommonChar("系", "xi");
        addCommonChar("细", "xi");
        addCommonChar("下", "xia");
        addCommonChar("夏", "xia");
        addCommonChar("先", "xian");
        addCommonChar("现", "xian");
        addCommonChar("线", "xian");
        addCommonChar("想", "xiang");
        addCommonChar("向", "xiang");
        addCommonChar("象", "xiang");
        addCommonChar("像", "xiang");
        addCommonChar("小", "xiao");
        addCommonChar("校", "xiao");
        addCommonChar("笑", "xiao");
        addCommonChar("些", "xie");
        addCommonChar("写", "xie");
        addCommonChar("谢", "xie");
        addCommonChar("新", "xin");
        addCommonChar("心", "xin");
        addCommonChar("信", "xin");
        addCommonChar("星", "xing");
        addCommonChar("行", "xing");
        addCommonChar("形", "xing");
        addCommonChar("醒", "xing");
        addCommonChar("姓", "xing");
        addCommonChar("休", "xiu");
        addCommonChar("修", "xiu");
        addCommonChar("需", "xu");
        addCommonChar("许", "xu");
        addCommonChar("学", "xue");
        addCommonChar("血", "xue");
        addCommonChar("牙", "ya");
        addCommonChar("言", "yan");
        addCommonChar("研", "yan");
        addCommonChar("眼", "yan");
        addCommonChar("演", "yan");
        addCommonChar("阳", "yang");
        addCommonChar("养", "yang");
        addCommonChar("样", "yang");
        addCommonChar("药", "yao");
        addCommonChar("要", "yao");
        addCommonChar("也", "ye");
        addCommonChar("夜", "ye");
        addCommonChar("业", "ye");
        addCommonChar("叶", "ye");
        addCommonChar("一", "yi");
        addCommonChar("医", "yi");
        addCommonChar("衣", "yi");
        addCommonChar("以", "yi");
        addCommonChar("已", "yi");
        addCommonChar("意", "yi");
        addCommonChar("易", "yi");
        addCommonChar("因", "yin");
        addCommonChar("音", "yin");
        addCommonChar("银", "yin");
        addCommonChar("印", "yin");
        addCommonChar("英", "ying");
        addCommonChar("影", "ying");
        addCommonChar("用", "yong");
        addCommonChar("由", "you");
        addCommonChar("有", "you");
        addCommonChar("又", "you");
        addCommonChar("右", "you");
        addCommonChar("鱼", "yu");
        addCommonChar("雨", "yu");
        addCommonChar("语", "yu");
        addCommonChar("元", "yuan");
        addCommonChar("原", "yuan");
        addCommonChar("远", "yuan");
        addCommonChar("院", "yuan");
        addCommonChar("愿", "yuan");
        addCommonChar("月", "yue");
        addCommonChar("越", "yue");
        addCommonChar("云", "yun");
        addCommonChar("运", "yun");
        addCommonChar("杂", "za");
        addCommonChar("在", "zai");
        addCommonChar("再", "zai");
        addCommonChar("早", "zao");
        addCommonChar("怎", "zen");
        addCommonChar("站", "zhan");
        addCommonChar("张", "zhang");
        addCommonChar("找", "zhao");
        addCommonChar("照", "zhao");
        addCommonChar("者", "zhe");
        addCommonChar("这", "zhe");
        addCommonChar("真", "zhen");
        addCommonChar("整", "zheng");
        addCommonChar("正", "zheng");
        addCommonChar("政", "zheng");
        addCommonChar("知", "zhi");
        addCommonChar("之", "zhi");
        addCommonChar("只", "zhi");
        addCommonChar("纸", "zhi");
        addCommonChar("指", "zhi");
        addCommonChar("至", "zhi");
        addCommonChar("治", "zhi");
        addCommonChar("中", "zhong");
        addCommonChar("州", "zhou");
        addCommonChar("主", "zhu");
        addCommonChar("住", "zhu");
        addCommonChar("注", "zhu");
        addCommonChar("祝", "zhu");
        addCommonChar("著", "zhu");
        addCommonChar("专", "zhuan");
        addCommonChar("转", "zhuan");
        addCommonChar("装", "zhuang");
        addCommonChar("准", "zhun");
        addCommonChar("子", "zi");
        addCommonChar("字", "zi");
        addCommonChar("自", "zi");
        addCommonChar("走", "zou");
        addCommonChar("租", "zu");
        addCommonChar("足", "zu");
        addCommonChar("组", "zu");
        addCommonChar("祖", "zu");
        addCommonChar("嘴", "zui");
        addCommonChar("最", "zui");
        addCommonChar("昨", "zuo");
        addCommonChar("左", "zuo");
        addCommonChar("作", "zuo");
        addCommonChar("座", "zuo");
        addCommonChar("做", "zuo");
    }
    
    /**
     * 向拼音映射表添加常用汉字
     * @param ch 汉字字符串
     * @param pinyin 拼音
     */
    private static void addCommonChar(String ch, String pinyin) {
        if (ch != null && ch.length() > 0 && pinyin != null) {
            PINYIN_MAP.put(ch.charAt(0), pinyin);
        }
    }
    
    /**
     * 获取所有数据来源系统
     * @return 所有数据来源系统列表
     */
    @Override
    public List<DataSourceSystem> getAllDataSourceSystems() {
        return dataSourceSystemRepository.findAll();
    }
    
    /**
     * 根据ID获取数据来源系统
     * @param id 系统ID
     * @return 系统对象，不存在返回null
     */
    @Override
    public DataSourceSystem getDataSourceSystemById(Long id) {
        return dataSourceSystemRepository.findById(id).orElse(null);
    }
    
    /**
     * 根据关键词模糊搜索数据来源系统
     * @param keyword 搜索关键词
     * @return 匹配的系统列表
     */
    @Override
    public List<DataSourceSystem> searchByKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return dataSourceSystemRepository.findAll();
        }
        return dataSourceSystemRepository.searchByKeyword(keyword.trim());
    }
    
    /**
     * 根据拼音首字母搜索数据来源系统
     * @param pinyinCode 拼音首字母
     * @return 匹配的系统列表
     */
    @Override
    public List<DataSourceSystem> searchByPinyinCode(String pinyinCode) {
        if (pinyinCode == null || pinyinCode.trim().isEmpty()) {
            return dataSourceSystemRepository.findAll();
        }
        return dataSourceSystemRepository.findByPinyinCodeStartingWith(pinyinCode.trim().toUpperCase());
    }
    
    /**
     * 创建数据来源系统
     * 自动生成拼音编码
     * @param dataSourceSystem 系统信息
     * @param userId 创建人ID
     * @return 创建后的系统对象
     */
    @Override
    public DataSourceSystem createDataSourceSystem(DataSourceSystem dataSourceSystem, Long userId) {
        if (dataSourceSystem.getSystemName() == null || dataSourceSystem.getSystemName().trim().isEmpty()) {
            throw new RuntimeException("数据来源系统名称不能为空");
        }
        
        dataSourceSystem.setSystemName(dataSourceSystem.getSystemName().trim());
        dataSourceSystem.setPinyinCode(generatePinyinCode(dataSourceSystem.getSystemName()));
        
        if (userId != null) {
            dataSourceSystem.setCreatedBy(userId);
            dataSourceSystem.setUpdatedBy(userId);
        }
        
        return dataSourceSystemRepository.save(dataSourceSystem);
    }
    
    /**
     * 更新数据来源系统
     * 自动重新生成拼音编码
     * @param id 系统ID
     * @param dataSourceSystem 更新的系统信息
     * @param userId 更新人ID
     * @return 更新后的系统对象
     */
    @Override
    public DataSourceSystem updateDataSourceSystem(Long id, DataSourceSystem dataSourceSystem, Long userId) {
        Optional<DataSourceSystem> existingOpt = dataSourceSystemRepository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new RuntimeException("数据来源系统不存在: " + id);
        }
        
        DataSourceSystem existing = existingOpt.get();
        
        if (dataSourceSystem.getSystemName() != null && !dataSourceSystem.getSystemName().trim().isEmpty()) {
            existing.setSystemName(dataSourceSystem.getSystemName().trim());
            existing.setPinyinCode(generatePinyinCode(dataSourceSystem.getSystemName()));
        }
        
        if (dataSourceSystem.getDescription() != null) {
            existing.setDescription(dataSourceSystem.getDescription());
        }
        
        if (userId != null) {
            existing.setUpdatedBy(userId);
        }
        
        return dataSourceSystemRepository.save(existing);
    }
    
    /**
     * 删除数据来源系统
     * @param id 系统ID
     */
    @Override
    public void deleteDataSourceSystem(Long id) {
        if (!dataSourceSystemRepository.existsById(id)) {
            throw new RuntimeException("数据来源系统不存在: " + id);
        }
        dataSourceSystemRepository.deleteById(id);
    }
    
    /**
     * 检查系统名称是否已存在
     * @param systemName 系统名称
     * @return 已存在返回true
     */
    @Override
    public boolean isSystemNameExists(String systemName) {
        if (systemName == null || systemName.trim().isEmpty()) {
            return false;
        }
        return dataSourceSystemRepository.existsBySystemName(systemName.trim());
    }
    
    /**
     * 生成拼音首字母编码
     * 支持英文字母、数字和汉字
     * @param systemName 系统名称
     * @return 拼音首字母编码（大写）
     */
    private String generatePinyinCode(String systemName) {
        if (systemName == null || systemName.isEmpty()) {
            return "";
        }
        
        StringBuilder pinyinCode = new StringBuilder();
        for (char c : systemName.toCharArray()) {
            if (c >= 'a' && c <= 'z') {
                // 小写字母直接添加
                pinyinCode.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                // 大写字母转小写
                pinyinCode.append((char)(c + 32));
            } else if (c >= '0' && c <= '9') {
                // 数字直接添加
                pinyinCode.append(c);
            } else if (c >= 0x4E00 && c <= 0x9FA5) {
                // 汉字，从拼音映射表获取首字母
                String pinyin = PINYIN_MAP.get(c);
                if (pinyin != null && !pinyin.isEmpty()) {
                    pinyinCode.append(pinyin.charAt(0));
                }
            }
        }
        return pinyinCode.toString().toUpperCase();
    }
}
