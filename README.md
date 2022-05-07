# RateLimitter
nftablesのlimit rateのような単位時間内でのアクセス数制限（流量制御）を行うクラスの実装です。

- 流量制御の単位は単なる文字列で指定するので、IPアドレス単位、IPアドレス＋ポート番号単位、ユーザID単位など、好きな単位での流量制御を行えます。
- インスタンスごとに最大アクセス数を別々に指定できます。A機能は最大で5分に1回、B機能は最大で1分に1回……といった指定も容易に行なえます。

## How to use

### A. Spring Web MVCのRestControllerクラスでリクエスト送信元IPアドレス単位の流量制御を行う場合

- コンストラクタで流量を指定しない場合、デフォルトの流量（最大10回/分）になります。
- ここではインスタンスを`static`変数に保持しています。これはリクエストの処理終了後もRateLimitterのインスタンスを保持する必要があるためです。
  `@Scope("request")`の場合、`static`は必須です。デフォルトのスコープ（`@Scope("singleton")`）の場合は`static`がなくても機能します。

``` java
@RestController
public class HelloController {
    private static RateLimitter limitter = new RateLimitter();

    @Autowired
    HttpServletRequest request;

    @GetMapping("/")
    public String index() {
        if (!limitter.allowAccess(request.getRemoteAddr(), System.currentTimeMillis())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        // DO BUSINESS LOGIC
    }
}
```


### B. Spring Web MVCのRestControllerクラスでリクエスト送信元IPアドレス＋ポート番号単位の流量制御を行う場合

- CGNなどを考慮して、リクエスト送信元IPアドレス＋ポート番号単位の流量制御を行う場合の例です。
  流量制御のキーは単なる文字列なので、キーにポート番号を含めれば実現できます。

``` java
@RestController
public class HelloController {
    private static RateLimitter limitter = new RateLimitter();

    @Autowired
    HttpServletRequest request;

    @GetMapping("/")
    public String index() {
        if (!limitter.allowAccess(request.getRemoteAddr() + ":" + request.getRemotePort(), System.currentTimeMillis())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        // DO BUSINESS LOGIC
    }
}
```


### C. クラスごとに流量を変えたい場合

- コンストラクタで流量を指定できます。
- TODO: RateLimitterをSpringのBeanとして定義して、流量制御の単位を設定ファイル（`ApplicationContext.xml`）から指定する例を追加したい。

``` java
@RestController
public class HelloController {
    private static RateLimitter limitter = new RateLimitter(60*1000, 1);

    @Autowired
    HttpServletRequest request;

    @GetMapping("/")
    public String index() {
        if (!limitter.allowAccess(request.getRemoteAddr(), System.currentTimeMillis())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        // DO BUSINESS LOGIC
    }
}
```

``` java
@RestController
public class GoodbyeController {
    private static RateLimitter limitter = new RateLimitter(60*1000, 3);

    @Autowired
    HttpServletRequest request;

    @GetMapping("/")
    public String index() {
        if (!limitter.allowAccess(request.getRemoteAddr(), System.currentTimeMillis())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        // DO BUSINESS LOGIC
    }
}
```



