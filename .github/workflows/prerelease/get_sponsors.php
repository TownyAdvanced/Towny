<?php
$file = 'sponsors.txt';
$sponsors = '';
$response = array();
$sponsorsUrl = "https://github.com/sponsors/LlmDl";
$html = file_get_contents($sponsorsUrl);
libxml_use_internal_errors( true);
$doc = new DOMDocument;
$doc->loadHTML( $html);
$xpath = new DOMXpath( $doc);
$node = $xpath->query( '//h4[@class="mb-3"]')->item( 0);
//echo $node->textContent; // This will print **GET THIS TEXT**
$totalSponsors = $node->textContent;
$res = preg_replace("/[^0-9]/", "", $totalSponsors );
$totalSponsorCount = intval($res);
$totalPages = ceil($totalSponsorCount / 54);
$publicSponsorCount = 0;
$testarray = array();

for($page = 1; $page <= $totalPages; $page++){
        $url = "https://github.com//sponsors/LlmDl/sponsors_partial?filter=active&page=".$page;
        $html = file_get_contents($url);
        $doc = new DOMDocument();
        $doc->loadHTML($html); //helps if html is well formed and has proper use of html entities!
        $xpath = new DOMXpath($doc);
        $nodes = $xpath->query('//a[@class="d-inline-block"]');
        foreach($nodes as $node) {
                $workingString = $node->getAttribute('href');
                $workedString = str_replace("/", "", $workingString);
                $sponsors .= $workedString . "\n";
                $publicSponsorCount++;
        }
}
$privateSponsorCount = $totalSponsorCount - $publicSponsorCount;
for ($private = 1; $private <= $privateSponsorCount; $private++){
        $sponsors .= "*privateSponsor" . "\n";
}
file_put_contents($file, $sponsors);
?>
