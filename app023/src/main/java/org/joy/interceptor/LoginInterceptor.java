package org.joy.interceptor;

import java.security.PrivateKey;

import javax.crypto.Cipher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

//...636p.로그인한 사용자 정보를 postHandle()을 이용하여 HttpSession 객체에 보관 처리함.
public class LoginInterceptor extends HandlerInterceptorAdapter {

  private static final String LOGIN = "login";
  private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);
  
  

  @Override
  public void postHandle(HttpServletRequest request, 
      HttpServletResponse response, Object handler,
      ModelAndView modelAndView) throws Exception {

    HttpSession session = request.getSession();

    ModelMap modelMap = modelAndView.getModelMap();
    Object userVO = modelMap.get("userVO");

    if (userVO != null) {

      logger.info("new login success");
      session.setAttribute(LOGIN, userVO);

      if (request.getParameter("useCookie") != null) {
        Cookie loginCookie = new Cookie("loginCookie", session.getId());
        loginCookie.setPath("/");
        loginCookie.setMaxAge(60 * 60 * 24 * 7); //...1week.
        logger.info("remember me :: loginCookie : "+loginCookie.toString());
        //...659p.만들어진 쿠기는 반드시 HttpServletResponse에 담아서 전송됨.
        response.addCookie(loginCookie);
      }
      
      //response.sendRedirect("/");      
      Object dest = session.getAttribute("dest");      
      response.sendRedirect(dest != null ? (String)dest:"/");
    }
  }

  // @Override
  // public void postHandle(HttpServletRequest request,
  // HttpServletResponse response, Object handler,
  // ModelAndView modelAndView) throws Exception {
  //
  // HttpSession session = request.getSession();
  //
  // ModelMap modelMap = modelAndView.getModelMap();
  // Object userVO = modelMap.get("userVO");
  //
  // if(userVO != null){
  //
  // logger.info("new login success");
  // session.setAttribute(LOGIN, userVO);
  // //response.sendRedirect("/");
  //
  // Object dest = session.getAttribute("dest");
  //
  // response.sendRedirect(dest != null ? (String)dest:"/");
  // }
  // }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

    HttpSession session = request.getSession();

    if (session.getAttribute(LOGIN) != null) {
      logger.info("clear login data before");
      session.removeAttribute(LOGIN);
    }
    
    return true;
  }

}
