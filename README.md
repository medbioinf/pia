PIA - Protein Inference Algorithms
===

PIA is a toolbox for MS based protein inference and identification analysis.

PIA allows you to inspect the results of common proteomics spectrum
identification search engines, combine them and conduct statistical analyses.
The main focus of PIA lays on the integrated inference algorithms, i.e.
concluding the proteins from a set of identified spectra. But PIA also allows
you to inspect your peptide spectrum matches, calculate FDR values across
different search engine results and visualize the correspondence between PSMs,
peptides and proteins.

![PIA - Logo](https://raw.github.com/wiki/mpc-bioinformatics/pia/pia_logo.png)

PIA in a nutshell
===

Most search engines for protein identification in MS/MS experiments return
protein lists, although the actual search yields a set of peptide spectrum
matches (PSMs). The step from PSMs to proteins is called “protein inference”.
If a set of identified PSMs supports the detection of more than one protein in
the searched database (“protein ambiguity”), usually only one representative
accession is reported. These representatives may differ according to the used
search engine and settings. Thus the protein lists of different search engines
generally cannot be compared with one another. PSMs of complementary search
engines are often combined to enhance the number of reported proteins or to
verify the evidence of a peptide, which is improved by detection with distinct
algorithms.

We developed an algorithm suite written in Java, including a fully
parametrisable web-interface (using JavaServer Faces), which combines PSMs from
different experiments and/or search engines, and reports consistent and thus
comparable results. None of the parameters for the inference, like filtering or
scoring, are fixed as in prior approaches, but held as flexible as possible, to
allow for any adjustments needed by the user.

PIA can be called via the command line, via
[GenericKnimeNodes](https://github.com/genericworkflownodes/GenericKnimeNodes) in
KNIME or using the JavaServer Faces web-interface.
