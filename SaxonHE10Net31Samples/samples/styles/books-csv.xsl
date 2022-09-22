<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
  
  <xsl:output method="text"/>
  
  <!-- This stylesheet outputs the book list as a CSV file -->
  
  <xsl:template match="BOOKLIST">
    <xsl:apply-templates select="BOOKS"/>
  </xsl:template>
  
  <xsl:template match="BOOKS">
    <xsl:text>Title,Author,Category,Stock-Value</xsl:text>
    <xsl:for-each select="ITEM">
      "<xsl:value-of select="TITLE"/>","<xsl:text/>
      <xsl:value-of select="AUTHOR"/>","<xsl:text/>
      <xsl:value-of select="@CAT"/>(<xsl:text/>
      <xsl:value-of select="if (id(@CAT)) then id(@CAT)/@DESC else 'Unclassified'"/>)","<xsl:text/>
      <xsl:value-of select="format-number(PRICE * QUANTITY,'######0.00')"/>"<xsl:text/>
    </xsl:for-each><xsl:text/>
  </xsl:template>
  
</xsl:stylesheet>	
