package org.example.bigevent.filter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class IngestRequestBoundaryFilterTest {
    MockHttpServletRequest request(int length) {
        var request=new MockHttpServletRequest("POST","/internal/v1/ingest/events");
        request.addHeader("X-Ingest-Service-Token","fixture"); request.setContent(new byte[length]); return request;
    }
    @Test void unknownContentLengthStillHasActualByteLimit() throws Exception {
        var request=new MockHttpServletRequest("POST","/internal/v1/ingest/events") {
            @Override public long getContentLengthLong() { return -1; }
        };
        request.addHeader("X-Ingest-Service-Token","fixture"); request.setContent(new byte[256001]);
        var response=new MockHttpServletResponse();
        new IngestRequestBoundaryFilter("fixture").doFilter(request,response,(a,b)->fail("must not enter MVC"));
        assertEquals(413,response.getStatus());
    }
    @Test void declaredOversizeRejected() throws Exception {
        var response=new MockHttpServletResponse();
        new IngestRequestBoundaryFilter("fixture").doFilter(request(256001),response,(a,b)->fail("must not enter MVC"));
        assertEquals(413,response.getStatus());
    }
    @Test void exactLimitIsReplayedIntactToDeserializer() throws Exception {
        byte[] body=new byte[256000]; java.util.Arrays.fill(body,(byte)'a');
        var request=request(0); request.setContent(body);
        new IngestRequestBoundaryFilter("fixture").doFilter(request,new MockHttpServletResponse(),(a,b)->assertArrayEquals(body,a.getInputStream().readAllBytes()));
    }
    @Test void invalidIdentityRejectedBeforeBodyRead() throws Exception {
        var request=new MockHttpServletRequest("POST","/internal/v1/ingest/events") {
            @Override public jakarta.servlet.ServletInputStream getInputStream() { throw new AssertionError("unauthorized body read"); }
        };
        var response=new MockHttpServletResponse();
        new IngestRequestBoundaryFilter("fixture").doFilter(request,response,(a,b)->fail("must not enter MVC"));
        assertEquals(403,response.getStatus());
    }
    @Test void compressedBodyRejectedBeforeParsing() throws Exception {
        var request=request(10); request.addHeader("Content-Encoding","gzip");
        var response=new MockHttpServletResponse();
        new IngestRequestBoundaryFilter("fixture").doFilter(request,response,(a,b)->fail("must not enter MVC"));
        assertEquals(415,response.getStatus());
    }
}
