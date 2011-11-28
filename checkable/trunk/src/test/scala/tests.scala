package amsa {
  import org.junit.Test
  import scalaxb.DataRecord
  import xsd._
  import xsd.ComplexTypeModelSequence1
  import javax.xml.namespace.QName

  @Test
  class CheckableTest {
    @Test
    def test {

      //val list = List(new CtsAvailCheckable(), new GoogleCheckable(), new SampleWebAppCheckable())
      //list.foreach { case c: checkable.Checkable => { println(c); println(c()) } }
      val x =
        <schema xmlns="http://www.w3.org/2001/XMLSchema" xmlns:tns="mine" xmlns:i="http://moten.david.org/util/xsd/simplified/appinfo" targetNamespace="mine" elementFormDefault="qualified" version="1.0">
          <element name="name" type="string"/>
          <element name="address" type="tns:address"/>
          <complexType name="address">
            <sequence>
              <element name="number" type="string"/>
              <element name="street" type="tns:street"/>
            </sequence>
          </complexType>
          <simpleType name="street">
            <restriction base="string"/>
          </simpleType>
        </schema>;

      val s = scalaxb.fromXML[Schema](getXml)

      val topLevelElements =
        s.schemasequence1.flatMap(_.arg1.value match {
          case y: TopLevelElement => Some(y)
          case _ => None
        })

      val topLevelComplexTypes = s.schemasequence1.flatMap(_.arg1.value match {
        case y: TopLevelComplexType => Some(y)
        case _ => None
      })

      val topLevelSimpleTypes = s.schemasequence1.flatMap(_.arg1.value match {
        case y: TopLevelSimpleType => Some(y)
        case _ => None
      })

      val schemaTypes =
        (topLevelComplexTypes.map(x => (x.name.get, x))
          ++ (topLevelSimpleTypes.map(x => (x.name.get, x)))).toMap;

      val xs = "http://www.w3.org/2001/XMLSchema"
      def qn(namespaceUri: String, localPart: String) = new QName(namespaceUri, localPart)
      val baseTypes = Set("decimal", "string", "integer", "date", "datetime", "boolean").map(qn(xs, _))
      println(s)
      println

      println("\ntopLevelComplexTypes:")
      println(topLevelComplexTypes)
      println("\ntopLevelSimpleTypes:")
      println(topLevelSimpleTypes)

      def unexpected(s: String) = throw new RuntimeException(s)

      println("\ntopLevelElements:")
      println(topLevelElements)
      println

      val rootElement = "person"
      val element = topLevelElements.find(
        _.name match {
          case Some(y) => y equals rootElement
          case None => false
        }).getOrElse(unexpected("did not find element " + rootElement))

      println(element)

      val elementType = element.typeValue.getOrElse(unexpected("type of element " + rootElement + " is missing"))
      //      allTypes.get(elementType)
      println("elementType = " + elementType)
    }

    val getXml =
      <xs:schema targetNamespace="http://org.moten.david/example" xmlns="http://org.moten.david/example" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:i="http://moten.david.org/util/xsd/simplified/appinfo">
        <xs:annotation i:numberItems="true"/>
        <xs:element name="person" type="person">
          <xs:annotation i:label="Personal details"/>
        </xs:element>
        <xs:complexType name="person">
          <xs:sequence>
            <xs:element name="name" type="xs:string">
              <xs:annotation i:label="Full name"/>
            </xs:element>
            <xs:element name="email" type="email">
              <xs:annotation i:validation="Invalid format" i:after="If you have concerns about privacy please see our &lt;a href=&quot;http://google.com&quot;&gt;privacy statement&lt;a&gt;"/>
            </xs:element>
            <xs:element name="date-of-birth" type="xs:date">
              <xs:annotation i:before="Date of birth is required for age based statistical analysis but will not be available publically" i:label="Date of birth" i:description="dd/mm/yyyy (dd=day, mm=month, yyyy=year)" i:validation="This field must of the form dd/mm/yyyy. For example, 12 March 2011 is 12/03/2011" i:after="If you have concerns about privacy please see our &lt;a href=&quot;http://google.com&quot;&gt;privacy statement&lt;a&gt;"/>
            </xs:element>
            <xs:element name="passport-no" type="passport-no">
              <xs:annotation i:label="Passport Number" i:description="L followed by digits" i:validation="This field must start with an L and then follow with digits only"/>
            </xs:element>
            <xs:element name="permanent-resident" type="xs:boolean"/>
            <xs:element name="last-submission" type="xs:dateTime" minOccurs="0"/>
            <xs:element name="lived-in" type="country" maxOccurs="unbounded">
              <xs:annotation i:description="Select the current country name for the location you lived in. You may select multiple locations by clicking on the Add button."/>
            </xs:element>
            <xs:element name="number-of-children" type="xs:integer" default="0"/>
            <xs:choice>
              <xs:element name="address" type="xs:string">
                <xs:annotation i:label="Address" i:lines="4" i:cols="40"/>
              </xs:element>
              <xs:element name="phone" type="xs:string" maxOccurs="unbounded"/>
            </xs:choice>
            <xs:element name="code" type="code">
              <xs:annotation i:label="Identifier Code" i:validation="This field must be a whole number between 10 and 20 inclusive"/>
            </xs:element>
            <xs:element name="story" type="xs:string" minOccurs="0">
              <xs:annotation i:lines="10" i:cols="50"/>
            </xs:element>
            <xs:element name="height-m" type="xs:decimal">
              <xs:annotation i:label="Height in metres"/>
            </xs:element>
            <xs:element name="termsAndConditionsUnderstood" type="alwaysTrue">
              <xs:annotation i:label="I acknowledge the &lt;a href=''&gt;Terms and Conditions&gt;"/>
            </xs:element>
          </xs:sequence>
        </xs:complexType>
        <xs:simpleType name="alwaysTrue">
          <xs:restriction base="xs:boolean">
            <xs:enumeration value="true"/>
          </xs:restriction>
        </xs:simpleType>
        <xs:simpleType name="passport-no">
          <xs:restriction base="xs:string">
            <xs:pattern value="L[0-9]+"/>
          </xs:restriction>
        </xs:simpleType>
        <!-- List source : http://geotags.com/iso3166/countries.html -->
        <xs:simpleType name="country">
          <xs:restriction base="xs:string">
            <xs:enumeration value="Afghanistan"/>
            <xs:enumeration value="Albania"/>
            <xs:enumeration value="Algeria"/>
            <xs:enumeration value="Andorra"/>
            <xs:enumeration value="Angola"/>
            <xs:enumeration value="Antigua &amp; Deps"/>
            <xs:enumeration value="Argentina"/>
            <xs:enumeration value="Armenia"/>
            <xs:enumeration value="Australia">
              <xs:annotation i:label="Van Diemen's Land"/>
            </xs:enumeration>
            <xs:enumeration value="Austria"/>
            <xs:enumeration value="Azerbaijan"/>
            <xs:enumeration value="Bahamas"/>
            <xs:enumeration value="Bahrain"/>
            <xs:enumeration value="Bangladesh"/>
            <xs:enumeration value="Barbados"/>
            <xs:enumeration value="Belarus"/>
            <xs:enumeration value="Belgium"/>
            <xs:enumeration value="Belize"/>
            <xs:enumeration value="Benin"/>
            <xs:enumeration value="Bhutan"/>
            <xs:enumeration value="Bolivia"/>
            <xs:enumeration value="Bosnia Herzegovina"/>
            <xs:enumeration value="Botswana"/>
            <xs:enumeration value="Brazil"/>
            <xs:enumeration value="Brunei"/>
            <xs:enumeration value="Bulgaria"/>
            <xs:enumeration value="Burkina"/>
            <xs:enumeration value="Burundi"/>
            <xs:enumeration value="Cambodia"/>
            <xs:enumeration value="Cameroon"/>
            <xs:enumeration value="Canada"/>
            <xs:enumeration value="Cape Verde"/>
            <xs:enumeration value="Central African Rep"/>
            <xs:enumeration value="Chad"/>
            <xs:enumeration value="Chile"/>
            <xs:enumeration value="China"/>
            <xs:enumeration value="Colombia"/>
            <xs:enumeration value="Comoros"/>
            <xs:enumeration value="Congo"/>
            <xs:enumeration value="Congo {Democratic Rep}"/>
            <xs:enumeration value="Costa Rica"/>
            <xs:enumeration value="Croatia"/>
            <xs:enumeration value="Cuba"/>
            <xs:enumeration value="Cyprus"/>
            <xs:enumeration value="Czech Republic"/>
            <xs:enumeration value="Denmark"/>
            <xs:enumeration value="Djibouti"/>
            <xs:enumeration value="Dominica"/>
            <xs:enumeration value="Dominican Republic"/>
            <xs:enumeration value="East Timor"/>
            <xs:enumeration value="Ecuador"/>
            <xs:enumeration value="Egypt"/>
            <xs:enumeration value="El Salvador"/>
            <xs:enumeration value="Equatorial Guinea"/>
            <xs:enumeration value="Eritrea"/>
            <xs:enumeration value="Estonia"/>
            <xs:enumeration value="Ethiopia"/>
            <xs:enumeration value="Fiji"/>
            <xs:enumeration value="Finland"/>
            <xs:enumeration value="France"/>
            <xs:enumeration value="Gabon"/>
            <xs:enumeration value="Gambia"/>
            <xs:enumeration value="Georgia"/>
            <xs:enumeration value="Germany"/>
            <xs:enumeration value="Ghana"/>
            <xs:enumeration value="Greece"/>
            <xs:enumeration value="Grenada"/>
            <xs:enumeration value="Guatemala"/>
            <xs:enumeration value="Guinea"/>
            <xs:enumeration value="Guinea-Bissau"/>
            <xs:enumeration value="Guyana"/>
            <xs:enumeration value="Haiti"/>
            <xs:enumeration value="Honduras"/>
            <xs:enumeration value="Hungary"/>
            <xs:enumeration value="Iceland"/>
            <xs:enumeration value="India"/>
            <xs:enumeration value="Indonesia"/>
            <xs:enumeration value="Iran"/>
            <xs:enumeration value="Iraq"/>
            <xs:enumeration value="Ireland {Republic}"/>
            <xs:enumeration value="Israel"/>
            <xs:enumeration value="Italy"/>
            <xs:enumeration value="Ivory Coast"/>
            <xs:enumeration value="Jamaica"/>
            <xs:enumeration value="Japan"/>
            <xs:enumeration value="Jordan"/>
            <xs:enumeration value="Kazakhstan"/>
            <xs:enumeration value="Kenya"/>
            <xs:enumeration value="Kiribati"/>
            <xs:enumeration value="Korea North"/>
            <xs:enumeration value="Korea South"/>
            <xs:enumeration value="Kosovo"/>
            <xs:enumeration value="Kuwait"/>
            <xs:enumeration value="Kyrgyzstan"/>
            <xs:enumeration value="Laos"/>
            <xs:enumeration value="Latvia"/>
            <xs:enumeration value="Lebanon"/>
            <xs:enumeration value="Lesotho"/>
            <xs:enumeration value="Liberia"/>
            <xs:enumeration value="Libya"/>
            <xs:enumeration value="Liechtenstein"/>
            <xs:enumeration value="Lithuania"/>
            <xs:enumeration value="Luxembourg"/>
            <xs:enumeration value="Macedonia"/>
            <xs:enumeration value="Madagascar"/>
            <xs:enumeration value="Malawi"/>
            <xs:enumeration value="Malaysia"/>
            <xs:enumeration value="Maldives"/>
            <xs:enumeration value="Mali"/>
            <xs:enumeration value="Malta"/>
            <xs:enumeration value="Marshall Islands"/>
            <xs:enumeration value="Mauritania"/>
            <xs:enumeration value="Mauritius"/>
            <xs:enumeration value="Mexico"/>
            <xs:enumeration value="Micronesia"/>
            <xs:enumeration value="Moldova"/>
            <xs:enumeration value="Monaco"/>
            <xs:enumeration value="Mongolia"/>
            <xs:enumeration value="Montenegro"/>
            <xs:enumeration value="Morocco"/>
            <xs:enumeration value="Mozambique"/>
            <xs:enumeration value="Myanmar, {Burma}"/>
            <xs:enumeration value="Namibia"/>
            <xs:enumeration value="Nauru"/>
            <xs:enumeration value="Nepal"/>
            <xs:enumeration value="Netherlands"/>
            <xs:enumeration value="New Zealand"/>
            <xs:enumeration value="Nicaragua"/>
            <xs:enumeration value="Niger"/>
            <xs:enumeration value="Nigeria"/>
            <xs:enumeration value="Norway"/>
            <xs:enumeration value="Oman"/>
            <xs:enumeration value="Pakistan"/>
            <xs:enumeration value="Palau"/>
            <xs:enumeration value="Panama"/>
            <xs:enumeration value="Papua New Guinea"/>
            <xs:enumeration value="Paraguay"/>
            <xs:enumeration value="Peru"/>
            <xs:enumeration value="Philippines"/>
            <xs:enumeration value="Poland"/>
            <xs:enumeration value="Portugal"/>
            <xs:enumeration value="Qatar"/>
            <xs:enumeration value="Romania"/>
            <xs:enumeration value="Russian Federation"/>
            <xs:enumeration value="Rwanda"/>
            <xs:enumeration value="St Kitts &amp; Nevis"/>
            <xs:enumeration value="St Lucia"/>
            <xs:enumeration value="Saint Vincent &amp; the Grenadines"/>
            <xs:enumeration value="Samoa"/>
            <xs:enumeration value="San Marino"/>
            <xs:enumeration value="Sao Tome &amp; Principe"/>
            <xs:enumeration value="Saudi Arabia"/>
            <xs:enumeration value="Senegal"/>
            <xs:enumeration value="Serbia"/>
            <xs:enumeration value="Seychelles"/>
            <xs:enumeration value="Sierra Leone"/>
            <xs:enumeration value="Singapore"/>
            <xs:enumeration value="Slovakia"/>
            <xs:enumeration value="Slovenia"/>
            <xs:enumeration value="Solomon Islands"/>
            <xs:enumeration value="Somalia"/>
            <xs:enumeration value="South Africa"/>
            <xs:enumeration value="Spain"/>
            <xs:enumeration value="Sri Lanka"/>
            <xs:enumeration value="Sudan"/>
            <xs:enumeration value="Suriname"/>
            <xs:enumeration value="Swaziland"/>
            <xs:enumeration value="Sweden"/>
            <xs:enumeration value="Switzerland"/>
            <xs:enumeration value="Syria"/>
            <xs:enumeration value="Taiwan"/>
            <xs:enumeration value="Tajikistan"/>
            <xs:enumeration value="Tanzania"/>
            <xs:enumeration value="Thailand"/>
            <xs:enumeration value="Togo"/>
            <xs:enumeration value="Tonga"/>
            <xs:enumeration value="Trinidad &amp; Tobago"/>
            <xs:enumeration value="Tunisia"/>
            <xs:enumeration value="Turkey"/>
            <xs:enumeration value="Turkmenistan"/>
            <xs:enumeration value="Tuvalu"/>
            <xs:enumeration value="Uganda"/>
            <xs:enumeration value="Ukraine"/>
            <xs:enumeration value="United Arab Emirates"/>
            <xs:enumeration value="United Kingdom"/>
            <xs:enumeration value="United States"/>
            <xs:enumeration value="Uruguay"/>
            <xs:enumeration value="Uzbekistan"/>
            <xs:enumeration value="Vanuatu"/>
            <xs:enumeration value="Vatican City"/>
            <xs:enumeration value="Venezuela"/>
            <xs:enumeration value="Vietnam"/>
            <xs:enumeration value="Yemen"/>
            <xs:enumeration value="Zambia"/>
            <xs:enumeration value="Zimbabwe"/>
          </xs:restriction>
        </xs:simpleType>
        <xs:simpleType name="code">
          <xs:restriction base="xs:integer">
            <xs:minExclusive value="9"/>
            <xs:maxExclusive value="21"/>
          </xs:restriction>
        </xs:simpleType>
        <xs:simpleType name="code2">
          <xs:restriction base="xs:integer">
            <xs:minInclusive value="10"/>
            <xs:maxInclusive value="20"/>
          </xs:restriction>
        </xs:simpleType>
      </xs:schema>;
  }

}