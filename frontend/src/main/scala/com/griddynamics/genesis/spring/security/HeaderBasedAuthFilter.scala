package com.griddynamics.genesis.spring.security

import org.springframework.web.filter.GenericFilterBean
import javax.servlet.{FilterChain, ServletResponse, ServletRequest}
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.griddynamics.genesis.http.TunnelFilter
import com.griddynamics.genesis.util.Logging
import org.springframework.security.core.{AuthenticationException, Authentication}
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.beans.factory.annotation.Autowired
import com.griddynamics.genesis.spring.ApplicationContextAware
import com.griddynamics.genesis.users.UserService
import org.springframework.beans.BeansException

class HeaderBasedAuthFilter extends GenericFilterBean with Logging with ApplicationContextAware {
    @Autowired var authenticationManager: AuthenticationManager = _
    lazy val userService: Option[UserService] = try {
        Some(applicationContext.getBean(classOf[UserService]))
    } catch {
        case e: BeansException => None
    }

    def doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request.asInstanceOf[HttpServletRequest]
        val assignedRoles: Array[String] =
            Option(httpRequest.getHeader(TunnelFilter.AUTH_HEADER_NAME)).map(_.split(TunnelFilter.SEPARATOR_CHAR)).getOrElse(Array(""))
        val assignedUser = Option(httpRequest.getHeader(TunnelFilter.SEC_HEADER_NAME))
        if (assignedUser.isDefined && authenticationRequired) {
            assignedUser.map(user => {
                val token = new ExternalAuthentication(user, assignedRoles.toList)
                try {
                    log.debug("Authenticating user %s", user)
                    val authentication: Authentication = authenticationManager.authenticate(token)
                    SecurityContextHolder.getContext.setAuthentication(authentication)
                } catch {
                    case e: AuthenticationException => {
                        log.debug("Authentication request failed: %s".format(e.getMessage))
                        response.asInstanceOf[HttpServletResponse].sendError(HttpServletResponse.SC_FORBIDDEN, "You are not allowed to access this resource")
                        SecurityContextHolder.clearContext()
                        return
                    }
                }
            })
        }
        chain.doFilter(request, response)
    }

    def authenticationRequired = {
        val existingAuth: Authentication = SecurityContextHolder.getContext.getAuthentication
        userService.isEmpty && (existingAuth == null || !existingAuth.isAuthenticated)
    }
}
