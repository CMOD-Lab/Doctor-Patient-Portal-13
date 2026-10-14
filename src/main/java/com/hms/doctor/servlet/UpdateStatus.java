package com.hms.doctor.servlet;

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

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;

/**
 * UpdateStatus – updates the doctor's appointment comment/status.
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
@WebServlet("/updateStatus")
public class UpdateStatus extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		try {

			int    id       = Integer.parseInt(req.getParameter("id"));
			int    doctorId = Integer.parseInt(req.getParameter("doctorId"));
			String comment  = req.getParameter("comment");

			AppointmentDAO appDAO = new AppointmentDAO(DBConnection.getConn());
			boolean f = appDAO.updateDrAppointmentCommentStatus(id, doctorId, comment);

			// cr-java-0065 (Line 30): HttpSession is now backed by Amazon ElastiCache for Redis
			// through Spring Session – setAttribute below operates on the distributed
			// Redis store, not in-process server memory.
			HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

			if (f == true) {
				// cr-java-0065 (Line 34): successMsg stored in Redis-backed distributed session
				session.setAttribute("successMsg", "Comment updated");
				resp.sendRedirect("doctor/patient.jsp");

			} else {
				// cr-java-0065 (Line 39): errorMsg stored in Redis-backed distributed session
				session.setAttribute("errorMsg", "Something went wrong on server!");
				resp.sendRedirect("doctor/patient.jsp");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
