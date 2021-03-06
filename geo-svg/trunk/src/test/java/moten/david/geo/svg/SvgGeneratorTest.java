package moten.david.geo.svg;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import moten.david.util.io.IoUtil;

import org.junit.Test;

public class SvgGeneratorTest {

    @Test
    public void test() throws IOException {
	SvgGenerator g = createSvgGenerator(true);
	g.add(new Point(144, -21));
	g.add(new Line(new Point(125, -10), new Point(138, -39)));
	FileOutputStream fos = new FileOutputStream("target/test.svg");
	fos.write(g.svg().getBytes());
	fos.close();
    }

    @Test
    public void testLotsOfDots() {
	FixParser parser = new FixParser();
	for (String line : IoUtil.getLines(getClass().getResourceAsStream(
		"/fixes.txt"))) {
	    System.out.println(line);
	    Fix fix;
	    if (line.trim().length() > 0)
		fix = parser.parse(new ByteArrayInputStream(line.getBytes()));
	}
    }

    @Test
    public void testTricky() {
	FixParser p = new FixParser();
	String xml = "<fix agent=\"AIS\" type=\"Vessel\" lat=\"-20.278567\" lon=\"116.2917\" kts=\"0.2\" course=\"36.0\"><identity name=\"MMSI\" value=\"233940000\"/><identity name=\"IMO Number\" value=\"9002817\"/><property name=\"Name\" value=\"FAR SKY\"/><property name=\"Type\" value=\"Tug\"/><property name=\"Heading\" value=\"0\"/><property name=\"Navigation\" value=\"at anchor\"/><property name=\"Destination\" value=\"&quot;1SKCD1M#P2K# 9L3H,Q\"/><property name=\"ETA\" value=\"812429\"/></fix>";
	System.out.println(xml);
	p.parse(new ByteArrayInputStream(xml.getBytes()));
    }

    public static SvgGenerator createSvgGenerator(boolean external) {
	int widthPixels = 1200;
	int heightPixels = 800;
	double horizontalExtentDegrees = 70;
	Point topLeft = new Point(100, 0);
	Projection p = new ProjectionWGS84(topLeft, horizontalExtentDegrees,
		widthPixels);
	SvgRenderer renderer = new SvgRendererImpl();
	SvgGenerator g = new SvgGenerator(1.8, "red", widthPixels,
		heightPixels, p, renderer);

	// add an image with the assumption that we are using the WGS84
	// projection
	double verticalExtentDegrees = heightPixels
		* (horizontalExtentDegrees / widthPixels);

	if (external)
	    g.add(createImageExternal(topLeft, widthPixels, heightPixels,
		    verticalExtentDegrees, horizontalExtentDegrees));
	else
	    g.add(createImageInternal(widthPixels, heightPixels));

	return g;
    }

    private static Image createImageExternal(Point topLeft, int widthPixels,
	    int heightPixels, double verticalExtentDegrees,
	    double horizontalExtentDegrees) {
	return new Image(
		new ScreenPoint(0, 0),
		widthPixels,
		heightPixels,
		// "../src/test/resources/australia.jpg"
		"http://vmap0.tiles.osgeo.org/wms/vmap0?LAYERS=basic&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&EXCEPTIONS=application/vnd.ogc.se_inimage&FORMAT=image/jpeg&SRS=EPSG:4326&BBOX="
			+ topLeft.getX()
			+ ","
			+ (topLeft.getY() - verticalExtentDegrees)
			+ ","
			+ (topLeft.getX() + horizontalExtentDegrees)
			+ ","
			+ topLeft.getY()
			// + "135,-22,146,-11"
			+ "&WIDTH=" + widthPixels + "&HEIGHT=" + heightPixels);
    }

    private static Image createImageInternal(int widthPixels, int heightPixels) {
	return new Image(new ScreenPoint(0, 0), widthPixels, heightPixels,
		"../src/test/resources/australia.jpg");
    }
}
