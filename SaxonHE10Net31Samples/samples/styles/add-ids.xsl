<xsl:transform
 xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
 version="2.0">
  
  <!-- This stylesheet adds id attributes to all item elements -->
  
  <xsl:output method="xml" indent="yes"/>

  <xsl:template match="*">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template match="item">
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:attribute name="id" select="generate-id()"/>
      <xsl:apply-templates/>
    </xsl:copy>
  </xsl:template>

</xsl:transform>	
