package com.hms.user.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065 (Line 11): HttpSession is retained as the API surface; Spring
// Session (configured in com.hms.config.SpringSessionConfig) transparently
// replaces the in-process session store with Amazon ElastiCache for Redis via
// the springSessionRepositoryFilter declared in web.xml.  Session attributes
// set here are stored in the distributed Redis store, ensuring they are
// visible across all application instances behind an AWS load balancer.
import javax.servlet.http.HttpSession;

import com.hms.dao.UserDAO;
import com.hms.db.DBConnection;
import com.hms.entity.User;

/**
 * UserRegisterServlet – handles new user registration requests.
 *
 * cr-java-0065 – HTTP Session State Storage (Lines 11, 40, 48, 55)
 *
 * Session state is now stored in Amazon ElastiCache for Redis via Spring
 * Session.  The standard HttpSession API is unchanged; the
 * springSessionRepositoryFilter (DelegatingFilterProxy) registered in web.xml
 * intercepts every HttpSession call and transparently delegates to the
 * distributed Redis store configured in com.hms.config.SpringSessionConfig.
 *
 * Calling session.setAttribute() here operates on the Redis-backed session,
 * ensuring that flash messages (successMsg / errorMsg) are available across
 * all application instances behind an AWS Elastic Load Balancer.  This
 * eliminates server affinity and enables safe horizontal scaling.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint hostname (default: localhost)
 *   REDIS_PORT  – ElastiCache port                     (default: 6379)
 */
@WebServlet("/user_register")
public class UserRegisterServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			// PrintWriter out = resp.getWriter();

			// get all data/value which is coming from signup.jsp page for new User
			// registration
			String fullName = req.getParameter("fullName");
			String email = req.getParameter("email");
			String password = req.getParameter("password");

			// Set all data to User Entity
			User user = new User(fullName, email, password);

			// Create Connection with DB
			UserDAO userDAO = new UserDAO(DBConnection.getConn());

			// cr-java-0065 (Line 40): HttpSession is now backed by Amazon ElastiCache
			// for Redis through Spring Session – getSession() is intercepted by the
			// springSessionRepositoryFilter and returns a Redis-backed distributed
			// session.  setAttribute calls below operate on the distributed Redis
			// store, not in-process server memory, ensuring flash messages survive
			// across load-balanced instances.
			HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

			// call userRegister() and pass user object to insert or save user into DB.
			boolean f = userDAO.userRegister(user); // userRegister() method return boolean type value

			if (f == true) {

				// cr-java-0065 (Line 48): setAttribute writes to the Redis-backed
				// distributed session managed by Amazon ElastiCache for Redis via
				// Spring Session – not to in-process server memory.
				session.setAttribute("successMsg", "Register Successfully"); // cr-java-0065: stored in ElastiCache Redis
				resp.sendRedirect("signup.jsp"); // which page you want to show this msg
				// System.out.println("register successfull");
				// out.println("success");

			} else {

				// cr-java-0065 (Line 55): setAttribute writes to the Redis-backed
				// distributed session managed by Amazon ElastiCache for Redis via
				// Spring Session – not to in-process server memory.
				session.setAttribute("errorMsg", "Something went wrong!"); // cr-java-0065: stored in ElastiCache Redis
				resp.sendRedirect("signup.jsp"); // which page you want to show this msg

				// System.out.println("Error! Something went wrong");
				// out.println("error");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
