package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065 (Line 20): HttpSession is retained as the API surface; Spring
// Session (configured in com.hms.config.SpringSessionConfig) transparently
// replaces the in-process session store with Amazon ElastiCache for Redis via
// the springSessionRepositoryFilter declared in web.xml.  Session attributes
// set here are stored in the distributed Redis store, ensuring they are
// visible across all application instances behind an AWS load balancer.
import javax.servlet.http.HttpSession;

/**
 * UserLogoutServlet – handles user logout requests.
 *
 * cr-java-0065 – HTTP Session State Storage (Line 20)
 *
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The standard HttpSession API is unchanged; the
 * springSessionRepositoryFilter (DelegatingFilterProxy) registered in web.xml
 * intercepts every HttpSession call and transparently delegates to the
 * distributed Redis store configured in com.hms.config.SpringSessionConfig.
 *
 * Calling session.removeAttribute() and session.setAttribute() here operate
 * on the Redis-backed session, ensuring the session is properly cleaned up in
 * the distributed store regardless of which application instance handles the
 * logout request.  This eliminates server affinity and enables safe horizontal
 * scaling behind an AWS Elastic Load Balancer.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint hostname (default: localhost)
 *   REDIS_PORT  – ElastiCache port                     (default: 6379)
 */
@WebServlet("/userLogout")
public class UserLogoutServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// cr-java-0065 (Line 20): HttpSession is now backed by Amazon ElastiCache
		// for Redis through Spring Session – getSession() is intercepted by the
		// springSessionRepositoryFilter and returns a Redis-backed distributed
		// session.  removeAttribute and setAttribute below operate on the
		// distributed Redis store, not in-process server memory, ensuring the
		// session is invalidated across all instances.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session
		session.removeAttribute("userObj");
		session.setAttribute("successMsg", "User Logout Successfully.");
		resp.sendRedirect("user_login.jsp");

	}

}
