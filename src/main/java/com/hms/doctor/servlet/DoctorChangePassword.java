package com.hms.doctor.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
// cr-java-0065: HttpSession is retained as the API surface; Spring Session
// (configured in com.hms.config.SpringSessionConfig) transparently replaces
// the in-process session store with Amazon ElastiCache for Redis via the
// springSessionRepositoryFilter declared in web.xml.  All session attributes
// (successMsg, errorMsg) are stored in the distributed Redis store, eliminating
// server affinity and enabling safe horizontal scaling across multiple instances.
import javax.servlet.http.HttpSession;

import com.hms.dao.DoctorDAO;
import com.hms.db.DBConnection;

/**
 * DoctorChangePassword – handles password change requests from the doctor panel.
 *
 * cr-java-0065: Session feedback attributes (successMsg / errorMsg) are now
 * stored in Amazon ElastiCache for Redis via Spring Session.  The standard
 * HttpSession API is unchanged; the springSessionRepositoryFilter registered in
 * web.xml intercepts every HttpSession call and transparently delegates to the
 * distributed Redis store configured in com.hms.config.SpringSessionConfig.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/doctorChangePassword")
public class DoctorChangePassword extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int doctorId = Integer.parseInt(req.getParameter("doctorId"));
		String newPassword = req.getParameter("newPassword");
		String oldPassword = req.getParameter("oldPassword");

		DoctorDAO doctorDAO = new DoctorDAO(DBConnection.getConn());

		// cr-java-0065: session is backed by Amazon ElastiCache for Redis via
		// Spring Session – all attributes set below are stored in the distributed
		// Redis store, not in local server memory.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

		if (doctorDAO.checkOldPassword(doctorId, oldPassword)) {

			if (doctorDAO.changePassword(doctorId, newPassword)) {

				// cr-java-0065: successMsg stored in Redis-backed distributed session
				session.setAttribute("successMsg", "Password change successfully.");
				resp.sendRedirect("doctor/edit_profile.jsp");

			} else {

				// cr-java-0065: errorMsg stored in Redis-backed distributed session
				session.setAttribute("errorMsg", "Something went wrong on server!");
				resp.sendRedirect("doctor/edit_profile.jsp");

			}

		} else {
			// cr-java-0065: errorMsg stored in Redis-backed distributed session
			session.setAttribute("errorMsg", "Old Password not match");
			resp.sendRedirect("doctor/edit_profile.jsp");

		}
	}

}
