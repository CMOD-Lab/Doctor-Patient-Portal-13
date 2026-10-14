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

import com.hms.dao.AppointmentDAO;
import com.hms.db.DBConnection;
import com.hms.entity.Appointment;

/**
 * AppointmentServlet – records a new patient appointment.
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
@WebServlet("/addAppointment")
public class AppointmentServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		int    userId          = Integer.parseInt(req.getParameter("userId"));
		String fullName        = req.getParameter("fullName");
		String gender          = req.getParameter("gender");
		String age             = req.getParameter("age");
		String appointmentDate = req.getParameter("appointmentDate");
		String email           = req.getParameter("email");
		String phone           = req.getParameter("phone");
		String diseases        = req.getParameter("diseases");
		int    doctorId        = Integer.parseInt(req.getParameter("doctorNameSelect"));
		String address         = req.getParameter("address");

		Appointment appointment = new Appointment(userId, fullName, gender, age, appointmentDate, email, phone, diseases, doctorId, address, "Pending");

		AppointmentDAO appointmentDAO = new AppointmentDAO(DBConnection.getConn());
		boolean f = appointmentDAO.addAppointment(appointment);

		// cr-java-0065 (Line 40): HttpSession is now backed by Amazon ElastiCache for Redis
		// through Spring Session – setAttribute below operates on the distributed
		// Redis store, not in-process server memory.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

		if (f == true) {
			// cr-java-0065 (Line 44): successMsg stored in Redis-backed distributed session
			session.setAttribute("successMsg", "Appointment is recorded Successfully.");
			resp.sendRedirect("user_appointment.jsp");

		} else {
			// cr-java-0065 (Line 51): errorMsg stored in Redis-backed distributed session
			session.setAttribute("errorMsg", "Something went wrong on server!");
			resp.sendRedirect("user_appointment.jsp");
		}

	}

}
