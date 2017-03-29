package io.github.arawn.servlet;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(urlPatterns = "/async", asyncSupported = true)
public class SimpleAsyncServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // 서블릿 컨테이너에게 이 요청은 비동기로 처리한다고 알린다
        AsyncContext asyncContext = request.startAsync(request, response);

        // 비동기 처리 시간제한을 설정한다
        asyncContext.setTimeout(60 * 1000);

        // 서블릿 컨테이너에게 이벤트를 전달 받기 위한 리스너를 작성한다
        asyncContext.addListener(new AsyncListener() {

            public void onComplete(AsyncEvent event) throws IOException {
                // 응답이 완료될 때 서블릿 컨테이너가 알려준다
            }

            public void onTimeout(AsyncEvent event) throws IOException {
                // AsyncContext.setTimeout()으로 설정한 시간을 넘어서면 서블릿 컨테이너가 알려준다
            }

            public void onError(AsyncEvent event) throws IOException {
                // 오류가 발생하면 서블릿 컨테이너가 알려준다
            }

            public void onStartAsync(AsyncEvent event) throws IOException {
                // 새로운 비동기 작업이 시작되면 서블릿 컨테이너가 알려준다
            }

        });

        // 비동기 작업을 시작한다
        asyncContext.start(new Runnable() {

            @Override
            public void run() {

                // 응답을 처리하는 곳


                asyncContext.complete();        // 응답을 종료한다
            }

        });

    }

}
