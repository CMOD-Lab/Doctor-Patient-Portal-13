package com.hms.user.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065 (Line 10): HttpSession is retained as the API surface; Spring Session
// (configured in com.hms.config.SpringSessionConfig) transparently replaces
// the in-process session store with Amazon ElastiCache for Redis via the
// springSessionRepositoryFilter declared in web.xml.  Session attributes set
// here are stored in the distributed Redis store, ensuring they are visible
// across all application instances behind a load balancer.
import javax.servlet.http.HttpSession;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * UserLoginServlet – handles user authentication requests.
 *
 * cr-java-0065: Session state is stored in Amazon ElastiCache for Redis via
 * Spring Session.  The standard HttpSession API is unchanged; the
 * springSessionRepositoryFilter registered in web.xml intercepts every
 * HttpSession call and transparently delegates to the distributed Redis store
 * configured in com.hms.config.SpringSessionConfig.  Calling
 * session.setAttribute() here operates on the Redis-backed session, ensuring
 * the authenticated user object (userObj) and flash messages (errorMsg) are
 * visible regardless of which application instance handles the subsequent
 * redirect.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/userLogin")
public class UserLoginServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String email    = req.getParameter("email");
		String password = req.getParameter("password");

		// cr-java-0065 (Line 25): HttpSession is now backed by Amazon ElastiCache
		// for Redis through Spring Session – getSession() is intercepted by the
		// springSessionRepositoryFilter and returns a Redis-backed distributed
		// session, eliminating server affinity and enabling horizontal scaling.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

		UserDAO userDAO = new UserDAO(DBConnection.getConn());
		User user = userDAO.loginUser(email, password);

		if (user != null) {
			// cr-java-0065 (Line 31): setAttribute stores the authenticated user
			// object in the distributed Redis session store via Spring Session,
			// not in local in-process server memory.
			session.setAttribute("userObj", user);
			resp.sendRedirect("index.jsp");
		} else {
			// cr-java-0065 (Line 35): setAttribute stores the error flash message
			// in the distributed Redis session store via Spring Session.
			session.setAttribute("errorMsg", "Invalid email or password");
			resp.sendRedirect("user_login.jsp");
		}
	}

}
