package com.hms.admin.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * AdminLogoutServlet – session state is now stored in Amazon ElastiCache for
 * Redis via Spring Session.  The HttpSession API is unchanged; Spring Session
 * transparently replaces the in-process session store with a distributed Redis
 * store, eliminating server affinity and enabling horizontal scaling.
 *
 * Required configuration (application.properties / environment variables):
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 *
 * Spring Session is activated by @EnableRedisHttpSession on
 * com.hms.config.SpringSessionConfig.
 */
@WebServlet("/adminLogout")
public class AdminLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		
		// HttpSession is now backed by Amazon ElastiCache for Redis through
		// Spring Session – removeAttribute and setAttribute operate on the
		// distributed Redis store, ensuring logout is reflected across all
		// application instances immediately.
		HttpSession session = req.getSession();
		session.removeAttribute("adminObj");
		//show message after logout
		session.setAttribute("successMsg", "Admin Logout Successfully");
		resp.sendRedirect("admin_login.jsp");
		
		
		
	}

	
}
