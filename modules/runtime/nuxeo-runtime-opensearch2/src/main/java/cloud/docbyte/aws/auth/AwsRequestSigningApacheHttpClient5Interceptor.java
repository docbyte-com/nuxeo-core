package cloud.docbyte.aws.auth;

import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpRequestInterceptor;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.HttpContext;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.signer.Signer;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * An {@link HttpRequestInterceptor} (HC5) that signs requests using any AWS {@link Signer}
 * and {@link AwsCredentialsProvider}.
 * <p>
 * Because HC5's async pipeline does not buffer the request body at interceptor time,
 * use {@link software.amazon.awssdk.auth.signer.Aws4UnsignedPayloadSigner} (sets
 * {@code X-Amz-Content-Sha256: UNSIGNED-PAYLOAD}). This is safe over HTTPS since TLS
 * guarantees body integrity.
 */
public class AwsRequestSigningApacheHttpClient5Interceptor implements HttpRequestInterceptor {

    private final String service;
    private final Signer signer;
    private final AwsCredentialsProvider awsCredentialsProvider;
    private final Region region;

    public AwsRequestSigningApacheHttpClient5Interceptor(String service, Signer signer,
            AwsCredentialsProvider awsCredentialsProvider, Region region) {
        this.service = service;
        this.signer = signer;
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.region = Objects.requireNonNull(region);
    }

    public AwsRequestSigningApacheHttpClient5Interceptor(String service, Signer signer,
            AwsCredentialsProvider awsCredentialsProvider, String region) {
        this(service, signer, awsCredentialsProvider, Region.of(region));
    }

    @Override
    public void process(HttpRequest request, EntityDetails entity, HttpContext context)
            throws HttpException, IOException {
        URI requestUri = parseRequestUri(request.getRequestUri());
        URI fullUri = buildFullUri(requestUri, context);

        SdkHttpFullRequest.Builder sdkRequestBuilder = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.fromValue(request.getMethod()))
                .uri(uriWithoutQuery(fullUri))
                .rawQueryParameters(parseQueryParams(requestUri))
                .headers(headersToMap(request.headerIterator()));

        ExecutionAttributes executionAttributes = new ExecutionAttributes()
                .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS,
                        awsCredentialsProvider.resolveCredentials())
                .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, service)
                .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, region);

        SdkHttpFullRequest signedRequest = signer.sign(sdkRequestBuilder.build(), executionAttributes);

        Header[] signedHeaders = signedRequest.headers().entrySet().stream()
                .flatMap(e -> e.getValue().stream().map(v -> (Header) new BasicHeader(e.getKey(), v)))
                .toArray(Header[]::new);
        request.setHeaders(signedHeaders);
    }

    private URI parseRequestUri(String rawUri) throws IOException {
        try {
            return new URI(rawUri);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI: " + rawUri, e);
        }
    }

    private URI buildFullUri(URI requestUri, HttpContext context) throws IOException {
        if (requestUri.isAbsolute()) {
            return requestUri;
        }
        var route = HttpClientContext.adapt(context).getHttpRoute();
        HttpHost targetHost = route != null ? route.getTargetHost() : null;
        if (targetHost == null) {
            throw new IOException("No target host in context for relative URI: " + requestUri);
        }
        try {
            String path = requestUri.getRawPath() != null ? requestUri.getRawPath() : "/";
            return new URI(targetHost.getSchemeName(), null, targetHost.getHostName(),
                    targetHost.getPort(), path, null, null);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI", e);
        }
    }

    private URI uriWithoutQuery(URI uri) throws IOException {
        try {
            return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(),
                    uri.getPath(), null, null);
        } catch (URISyntaxException e) {
            throw new IOException("Invalid URI", e);
        }
    }

    private Map<String, List<String>> parseQueryParams(URI uri) {
        Map<String, List<String>> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String rawQuery = uri.getRawQuery();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int idx = pair.indexOf('=');
            String name = idx > 0 ? pair.substring(0, idx) : pair;
            String value = idx > 0 && idx < pair.length() - 1 ? pair.substring(idx + 1) : "";
            params.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
        return params;
    }

    private Map<String, List<String>> headersToMap(Iterator<Header> headers) {
        Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        while (headers.hasNext()) {
            Header header = headers.next();
            if (!skipHeader(header)) {
                map.computeIfAbsent(header.getName(), k -> new LinkedList<>()).add(header.getValue());
            }
        }
        return map;
    }

    private boolean skipHeader(Header header) {
        return ("content-length".equalsIgnoreCase(header.getName()) && "0".equals(header.getValue()))
                || "host".equalsIgnoreCase(header.getName());
    }
}
