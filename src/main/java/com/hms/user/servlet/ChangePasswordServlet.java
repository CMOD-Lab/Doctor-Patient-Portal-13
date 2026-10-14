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

/**
 * ChangePasswordServlet – handles user password change requests.
 *
 * cr-java-0065: Session state is stored in Amazon ElastiCache for Redis via
 * Spring Session.  The standard HttpSession API is unchanged; the
 * springSessionRepositoryFilter registered in web.xml intercepts every
 * HttpSession call and transparently delegates to the distributed Redis store
 * configured in com.hms.config.SpringSessionConfig.  Calling
 * session.setAttribute() here operates on the Redis-backed session, ensuring
 * flash messages (successMsg / errorMsg) are visible regardless of which
 * application instance handles the subsequent redirect.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/userChangePassword")
public class ChangePasswordServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId      = Integer.parseInt(req.getParameter("userId"));
		String oldPassword = req.getParameter("oldPassword");
		String newPassword = req.getParameter("newPassword");

		UserDAO uDAO = new UserDAO(DBConnection.getConn());

		// cr-java-0065: HttpSession is now backed by Amazon ElastiCache for Redis
		// through Spring Session – setAttribute below operates on the distributed
		// Redis store, not in-process server memory.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

		if (uDAO.checkOldPassword(userId, oldPassword)) {

			if (uDAO.changePassword(userId, newPassword)) {

				session.setAttribute("successMsg", "Password Change Successfully.");
				resp.sendRedirect("change_password.jsp");

			} else {

				session.setAttribute("errorMsg", "Something wrong on server!");
				resp.sendRedirect("change_password.jsp");

			}

		} else {
			session.setAttribute("errorMsg", "Old password incorrect");
			resp.sendRedirect("change_password.jsp");
		}

	}

}
