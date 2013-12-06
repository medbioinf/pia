/**
 * This file contains some JavaScript sources for PIA's web frontend
 * 
 */


/**
 * Draws a barchart in holder with the given data and labels
 * @param holder the HTML element for this chart
 * @param data_and_labels an array containing an array of data and an array of
 * the respective labels
 */
function drawPPMChart(holder, data_and_labels, labelled) {
	var data = data_and_labels[0];
	var r = Raphael(holder, 200, 120);
	var chart = r.barchart(0, 0, 200, 120, [data], 0, {});
	
	labelled = labelled || false;
	if (labelled) {
		// labeling of barcharts does not work properly in g.raphael
	    labels = data_and_labels[1] || [];
	    chart.labels = r.set();
	    var i = 0;
	    for (var j = 0; j < chart.bars[0].length; j++) {
	        var totX = 0;
	        for (i = 0; i < chart.bars.length; i++) {
	            totX += chart.bars[i][j].x;
	            y = chart.bars[0][j].y + chart.bars[0][j].h + 10;
	        }
	        x = totX / chart.bars.length;
	        
	        if (labels[j] != null) {
	        	r.text(x, y, labels[j]).attr("font","9px sans-serif");
	        }
	    }
	}
}


/**
 * Draws a chart for the number of identifications per PSM set
 * @param holder
 * @param data
 * @param label
 */
function drawIdentificationsChart(holder, data) {
	var legend_format = [];
	for (var i = 0; i < data.length; i++) {
		legend_format.push(''.concat(i+1).concat(" - %%.%% (##)"));
	}
	
	var r = Raphael(holder, 200, 120);
	r.piechart(35, 60, 35, data, { legend: legend_format, legendpos: "east"}).
			attr({font : "9px sans-serif"});
}


/**
 * Draws a chart for the number of identifications per peptides
 * @param holder
 * @param data
 * @param label
 */
function drawPeptideIdentificationsChart(holder, data, label) {
	var legend_format = [];
	for (var i = 0; i < data.length; i++) {
		legend_format.push(''.concat(label[i]).concat(" - ##"));
	}
	
	var r = Raphael(holder, 200, 120);
	r.piechart(60, 60, 60, data,
			{ legend: legend_format, legendpos: "east", maxSlices: 5}).
			attr({font : "9px sans-serif"});
}


/**
 * Draws a pie chart showing the unique and ambiguous PSM identifications.
 * @param holder
 * @param unique
 * @param all
 */
function drawUniqueChart(holder, unique, all) {
	var r = Raphael(holder, 300, 120);
	r.piechart(60, 60, 50, [unique, all - unique],
			{ legend: ["%%.%% - unique (##)", "%%.%% - ambiguous (##)"], legendpos: "east"});
}



/**
 * This function makes an embedded SVG image inline, so it can be edited by
 * other jQuery calls.
 */
$.fn.svginlineandselactable = function() {
	var $img = $(this);
	var imgID = $img.attr('id');
	var imgClass = $img.attr('class');
	var imgURL = $img.attr('src');
	
	jQuery.get(imgURL, function(data) {
		//Get the SVG tag, ignore the rest
		var $svg = jQuery(data).find('svg');
		
		// Add replaced image's ID to the new SVG
		if(typeof imgID !== 'undefined') {
			$svg = $svg.attr('id', imgID);
		}
		
        // Add replaced image's classes to the new SVG
		if(typeof imgClass !== 'undefined') {
			$svg = $svg.attr('class', imgClass + ' replaced-svg');
		}
		
		// Remove any invalid XML tags as per http://validator.w3.org
		$svg = $svg.removeAttr('xmlns:a');
		
		// Replace image with new SVG
		$img.replaceWith($svg).each(function() {
			$svg.selectableedges();
		});
	}, 'xml');
};


/**
 * This makes the edges of an GraphViz SVG graph selectable, either by clicking
 * it directly or by clicking on a connected node.
 */
$.fn.selectableedges = function () {
	var arrow = "->";
	
	function getEdges(nodeTitle, io) {
		if (io && io === "input") {
			nodeTitle = nodeTitle + arrow;
		} else if (io && io === "output") {
			nodeTitle = arrow + nodeTitle;
		} else {
			return null;
		}
		var titles = $("g.edge title").filter(function (idx) {
			return this.textContent.indexOf(nodeTitle) !== -1;
		});
		return titles.parent();
	}
	
	function thinEgde(edge) {
		edge.children("path").attr("stroke", "black").css("stroke-width", "");
		edge.children("polygon").attr("stroke", "black").attr("fill", "black");
	}
	
	function thickEgde(edge) {
		edge.children("path").attr("stroke", "red").css("stroke-width", "2px");
		edge.children("polygon").attr("stroke", "red").attr("fill", "red");
	}
	
	$(this).children().children("g").each(function () {
		if (this.className.baseVal === "node") {
			
			$(this).click(function () {
				var title = $(this).children("title").text();
				
				// make all edges thin again
				$(this).parent().children("g").each(function () {
					if (this.className.baseVal === "edge") {
						thinEgde($(this));
					}
				});
				
				getEdges(title, "input").each(function () {
					// and these one thick
					thickEgde($(this));
				});
			});
			
			$(this).dblclick(function () {
				var title = $(this).children("title").text();
				
				// make all edges thin again
				$(this).parent().children("g").each(function () {
					if (this.className.baseVal === "edge") {
						thinEgde($(this));
					}
				});
				
				getEdges(title, "output").each(function () {
					// and these one thick
					thickEgde($(this));
				});
			});
			
		} else if (this.className.baseVal === "edge") {
			// clicking on an edge highlights it in red
			$(this).click(function () {
				// make all edges thin again
				$(this).parent().children("g").each(function () {
					if (this.className.baseVal === "edge") {
						thinEgde($(this));
					}
				});
				// and this one thick
				thickEgde($(this));
            });
		}
	});
};
