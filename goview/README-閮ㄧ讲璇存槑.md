# GoView 婧愮爜閮ㄧ讲璇存槑锛圵indows锛?
鏈帇缂╁寘涓?**GoView 澶у睆鍙鍖?* 鍓嶅悗绔畬鏁存簮鐮侊紝鎺ユ敹鏂归渶鑷瀹夎渚濊禆骞朵慨鏀归厤缃悗杩愯銆?
## 1. 鐩綍缁撴瀯

```
goview-source/
鈹溾攢鈹€ README-閮ㄧ讲璇存槑.md           鈫?鏈枃浠?鈹溾攢鈹€ go-view-master-fetch/        鈫?鍓嶇锛圴ue 3 + Vite锛?鈹溾攢鈹€ go-view-serve-master/        鈫?鍚庣锛圫pring Boot + SQLite锛?鈹?  鈹斺攢鈹€ sqllite/goview.db        鈫?鏁版嵁搴擄紙椤圭洰涓庣敤鎴锋暟鎹級
鈹斺攢鈹€ upload/                      鈫?鍥剧墖/璧勬簮涓婁紶鐩綍
```

## 2. 鐜瑕佹眰

| 宸ュ叿 | 鐗堟湰 |
|------|------|
| Node.js | 鈮?16.14 |
| pnpm 鎴?npm | 浠婚€夊叾涓€ |
| JDK | 1.8 |
| Maven | 3.6+ |

寤鸿瀹夎璺緞涓嶅惈涓枃涓庣┖鏍笺€?
## 3. 瑙ｅ帇涓庤矾寰勯厤缃?
灏嗘暣涓枃浠跺す瑙ｅ帇鍒板浐瀹氫綅缃紝渚嬪锛?
```
D:\goview-source\
```

涓嬫枃鐢?`{ROOT}` 琛ㄧず瑙ｅ帇鏍圭洰褰曪紙璇锋浛鎹负浣犵殑瀹為檯璺緞锛學indows 閰嶇疆涓娇鐢?`//` 鎴?`\` 浣滀负璺緞鍒嗛殧锛夈€?
### 3.1 鍚庣 鈥?鏁版嵁搴撹矾寰?
缂栬緫 `go-view-serve-master\src\main\resources\application-dev.yml`锛?
```yaml
spring:
  datasource:
    url: jdbc:sqlite:D://goview-source//go-view-serve-master//sqllite//goview.db
```

### 3.2 鍚庣 鈥?涓婁紶鐩綍涓庤闂湴鍧€

缂栬緫 `go-view-serve-master\src\main\resources\application.yml` 涓?`v2` 鑺傜偣锛?
```yaml
v2:
  xnljmap:
    oss: file:D://goview-source//upload//
  fileurl: D://goview-source//upload
  httpurl: http://127.0.0.1:8083/
```

娉ㄦ剰锛歚oss` 浠?`file:` 寮€澶翠笖璺緞鏈熬鏈?`/`锛沗fileurl` 鏃?`file:` 鍓嶇紑涓旀湯灏炬棤 `/`銆?
### 3.3 鍓嶇 鈥?鍚庣 API 鍦板潃

澶嶅埗骞堕噸鍛藉悕锛?
```
go-view-master-fetch\.env.example  鈫? go-view-master-fetch\.env
```

纭 `.env` 涓細

```env
VITE_DEV_PATH = 'http://localhost:8083'
```

## 4. 鍚姩椤哄簭

**鍏堝惎鍚庣锛屽啀鍚墠绔€?*

### 4.1 鍚庣锛堢鍙?8083锛?
```powershell
cd go-view-serve-master
mvn clean package -DskipTests
java -jar target\goview_admin-0.0.1-SNAPSHOT.war
```

鍚姩鎴愬姛鍚庤闂帴鍙ｆ枃妗ｏ細http://localhost:8083/doc.html

### 4.2 鍓嶇锛堝紑鍙戞ā寮忥紝绔彛 3000锛?
鏂板紑涓€涓粓绔細

```powershell
cd go-view-master-fetch
pnpm install
pnpm dev
```

娴忚鍣ㄦ墦寮€锛歨ttp://localhost:3000

### 4.3 鐢熶骇鏋勫缓锛堝彲閫夛級

```powershell
cd go-view-master-fetch
pnpm build
```

浜х墿鍦?`dist\`锛屽彲閮ㄧ讲鍒?Nginx 绛夐潤鎬佹湇鍔″櫒锛涙鏃堕渶灏?`.env` 涓?`VITE_PRO_PATH` 鏀逛负鐢熶骇鐜鍚庣鍦板潃銆?
## 5. 榛樿鐧诲綍

| 椤?| 鍊?|
|----|-----|
| 璐﹀彿 | admin |
| 瀵嗙爜 | 123456 |

API 鍓嶇紑锛歚/api/goview`

## 6. 鏈旈粍閾佽矾纰虫帓鏀惧叏鏅ぇ灞?
鏈寘鍐呯疆 **鏈旈粍閾佽矾纰虫帓鏀惧叏鏅ぇ灞?* 婕旂ず椤圭洰锛堟暟鎹瓨浜?`sqllite\goview.db` 鐨?`t_goview_project_data.content`锛夈€?
| 椤?| 鍊?|
|----|-----|
| 椤圭洰鍚嶇О | 鏈旈粍閾佽矾纰虫帓鏀惧叏鏅ぇ灞?|
| 椤圭洰 ID | `2066429772333690882` |
| 棰勮鍦板潃锛堝紑鍙戞ā寮忥級 | http://localhost:3000/#/chart/preview/2066429772333690882 |

鐧诲綍 GoView 鍚庝篃鍙湪椤圭洰鍒楄〃涓墦寮€鍚屽悕椤圭洰銆?
鑻ユ暟鎹簱涓己灏戣椤圭洰鎴栭渶鐢ㄦ渶鏂版ā鏉胯鐩栫敾甯冨唴瀹癸紝鍦ㄥ悗绔凡鍚姩鐨勫墠鎻愪笅鎵ц锛?
```powershell
cd go-view-master-fetch
pnpm install
pnpm run dashboard:upsert
```

鑴氭湰浼氭寜椤圭洰 ID 鏇存柊澶у睆 JSON锛屼笉浼氭柊寤洪噸澶嶉」鐩€?
## 7. 鑷畾涔夌粍浠惰鏄?
鏈」鐩湪鍓嶇鎵╁睍浜?**鏈旈粍閾佽矾纰冲湴鍥?* 绛夌粍浠讹紝璺緞锛?
```
go-view-master-fetch\src\packages\components\Charts\Maps\RailwayCarbonMap\
```

濡傞渶浠?OpenStreetMap 閲嶆柊瀵煎叆绾胯矾鏁版嵁锛堥渶鑱旂綉锛夛細

```powershell
cd go-view-master-fetch
npm run import:shuohuang-railway
```

## 8. 甯歌闂

**Q: 鍓嶇鐧诲綍澶辫触 / 鎺ュ彛 404**  
A: 纭鍚庣宸插惎鍔ㄥ湪 8083锛屼笖 `.env` 涓?`VITE_DEV_PATH` 鎸囧悜 `http://localhost:8083`銆?
**Q: 涓婁紶鍥剧墖鏃犳硶鏄剧ず**  
A: 妫€鏌?`application.yml` 涓?`v2.oss`銆乣v2.fileurl`銆乣v2.httpurl` 鏄惁涓?`{ROOT}\upload` 涓€鑷淬€?
**Q: 鏁版嵁搴撴姤閿?*  
A: 妫€鏌?`application-dev.yml` 涓?SQLite 璺緞鏄惁鎸囧悜鏈寘鍐?`sqllite\goview.db` 鐨勭粷瀵硅矾寰勩€?
**Q: Maven 鏋勫缓澶辫触**  
A: 纭 JDK 1.8 涓?Maven 宸查厤缃?`JAVA_HOME`锛涢娆℃瀯寤洪渶涓嬭浇渚濊禆锛岃淇濇寔缃戠粶鐣呴€氥€?
## 9. 鐗堟湰淇℃伅

- 鍓嶇 go-view锛?.2.9锛堣 `go-view-master-fetch\package.json`锛?- 鍚庣 goview_admin锛?.0.1-SNAPSHOT锛堣 `go-view-serve-master\pom.xml`锛?
---
鎵撳寘鏃ユ湡锛歿DATE}
