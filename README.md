# RateLimitter
nftablesのlimit rateのような単位時間内でのアクセス数制限を行うクラスの実装です。

## 実装例

``` java
@RestController
public class HelloController {
    private static RateLimitter limitter = new RateLimitter();

    @Autowired
    HttpServletRequest request;

    @GetMapping("/")
    public String index() {
        if (!limitter.allowAccess(new Access(request.getRemoteAddr(), System.currentTimeMillis()))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS);
        }
        // DO BUSINESS LOGIC
    }
}
```
