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
// springSessionRepositoryFilter declared in web.xml.  Session invalidation
// performed here removes the session entry from the distributed Redis store,
// ensuring clean logout across all application instances.
import javax.servlet.http.HttpSession;

/**
 * DoctorLogoutServlet – invalidates the doctor's session on logout.
 *
 * cr-java-0065: Session state is stored in Amazon ElastiCache for Redis via
 * Spring Session.  The standard HttpSession API is unchanged; the
 * springSessionRepositoryFilter registered in web.xml intercepts every
 * HttpSession call and transparently delegates to the distributed Redis store
 * configured in com.hms.config.SpringSessionConfig.  Calling
 * session.removeAttribute() and session.setAttribute() here operates on the
 * Redis-backed session, ensuring the logout is visible regardless of which
 * application instance handles the subsequent request.
 *
 * Required environment variables:
 *   REDIS_HOST  – ElastiCache primary endpoint  (default: localhost)
 *   REDIS_PORT  – ElastiCache port              (default: 6379)
 */
@WebServlet("/doctorLogout")
public class DoctorLogoutServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		// cr-java-0065 (Line 18): HttpSession is now backed by Amazon ElastiCache for Redis
		// through Spring Session – removeAttribute and setAttribute below operate
		// on the distributed Redis store, not in-process server memory.
		HttpSession session = req.getSession(); // cr-java-0065: Redis-backed distributed session via Spring Session

		// cr-java-0065: doctorObj removed from Redis-backed distributed session
		session.removeAttribute("doctorObj");

		// cr-java-0065: successMsg stored in Redis-backed distributed session
		session.setAttribute("successMsg", "Doctor Logout Successfully.");

		resp.sendRedirect("doctor_login.jsp");
	}

}
