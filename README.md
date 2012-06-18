Winterface seperates logic and template of Freenet's web interface, enforces a better modularization and makes theming easier.

# Rationale
Current Freenet's web interface FProxy uses <tt>HTMLNodes</tt> in combination with <tt>ToadletServer</tt> to deliver HTML-Pages. This has the disadvantage of mixed template and logic which makes it hard to separately make changes to each of them. Moreover debugging and understanding of code can be very exhausting.

# Overview
Winterface is delivered as a Fred plugin:

* It uses Apache Wicket as its component-based web-framework to generate HTML files from templates
* Jetty (embedded) as a serlvet-container is used to deliver Wicket generated servlets.
* It should completely replace replace FProxy,ToadletServer and associated Toadlets
* It ''should'' make it possible to override Templates (HTML files) and design (CSS+JS)

What is to do:

* Create HTML templates and corresponding Wicket logic for each existing Toadlet
* Make reusable Wicket component (e.g. Panels) for reusable templates (e.g. Alerts)
* Eventually add new functionalities

# Winterface Workflow
The following is the general workflow of Winterface:

1. <tt>WinterPlugin</tt> is started by Fred and it starts Jetty
2. When Jetty is stated:
	2.1. A <tt>WicketFilter</tt> is configured to handle Wicket-related requests
	2.2. A resource servlet is configured to handle static resources (<tt>WicketFilter</tt> fallbacks to this servlet)
	2.3. <tt>FreenetWrapper</tt> is initiated and put into servlet container
3. On requests:
	3.1. A <tt>WicketServlet</tt> (handled by framework) looks for responsible <tt>WinterPage</tt> (subclass of <tt>WebPage</tt>)
	3.2 <tt>WinterPage</tt> has an associated HTML-Markup and dynamically generates content and returns an HTML page

# Components
Wicket uses HTML templates (with its own XML attributes/elements) in order to dynamically manipulate/generate HTML markup. Components follow the MVC pattern and use a model to generate their content. This however causes more amount of code, since the template and logic are strictly separated.

## WinterPage
<tt>WinterPage</tt> is a subclass of <tt>WebPage</tt>. Each WinterPage corresponds to a Toadlet with the difference, that It has its own Markup, which elements are manipulated upon each request.

## Panels
<tt>Panel</tt>s are reusable components with their own markup. Winterface starting page, Dashboard, uses for example various panels.

