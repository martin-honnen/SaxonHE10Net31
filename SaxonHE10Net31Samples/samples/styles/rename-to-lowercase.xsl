<xsl:transform
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="2.0">
  
  <!-- This stylesheet copies the input XML file, but converts names of elements and attributes to 
    lower-case -->
  
  <xsl:output method="xml" indent="yes"/>
  
  <xsl:template match="BOOKLIST">
    <xsl:element name="{lower-case(name(.))}">
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*">
    <xsl:element name="{lower-case(name(.))}">
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates/>
    </xsl:element>
  </xsl:template>
  
  <xsl:template match="@*">
    <xsl:attribute name="{lower-case(name(.))}" select="string(.)"/>
  </xsl:template>

</xsl:transform>	
