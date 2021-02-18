package sopro;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

@RestController
@RequestMapping("/**")
public class SoproController {
    public static final String API_PREFIX = "https://api.stackexchange.com";

    private static String getFullUri(HttpServletRequest request) {
        String uri = API_PREFIX + request.getRequestURI();
        String query = request.getQueryString();
        if (query == null) {
            return uri;
        } else {
            return uri + "?" + query;
        }
    }

    private final ConcurrentHashMap<String, ResponseEntity<String>> cache = new ConcurrentHashMap<>();

    @GetMapping
    public ResponseEntity<String> get(HttpServletRequest request) {
        String fullUri = getFullUri(request);
        System.out.println(fullUri);

        return cache.computeIfAbsent(fullUri, fullUri2 -> {
            System.out.println("Absent");
            try {
                var restTemplate = new RestTemplate();
                var raw = restTemplate.getForObject(fullUri2, byte[].class);
                // For some reason, Java HTTP clients fail to unzip automatically...
                var zip = new GZIPInputStream(new ByteArrayInputStream(raw));
                var out = new ByteArrayOutputStream();
                var buf = new byte[1024];
                int n;
                while ((n = zip.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(out.toString());
            } catch (Throwable ex) {
                return ResponseEntity.badRequest().body(ex.toString());
            }
        });
    }
}
