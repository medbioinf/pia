# PIA - Protein Inference Algorithms

PIA is a toolbox for MS based protein inference and identification analysis.

PIA allows you to inspect the results of common proteomics spectrum
identification search engines, combine them seamlessly and conduct statistical
analyses.
The main focus of PIA lays on the integrated inference algorithms, i.e.
concluding the proteins from a set of identified spectra. But it also allows
you to inspect your peptide spectrum matches, calculate FDR values across
different search engine results and visualize the correspondence between PSMs,
peptides and proteins.

![PIA - Logo](https://github.com/mpc-bioinformatics/pia/wiki/pia_logo.png)


## PIA in a nutshell

Most search engines for protein identification in MS/MS experiments return
protein lists, although the actual search yields a set of peptide spectrum
matches (PSMs). The step from PSMs to proteins is called "protein inference".
If a set of identified PSMs supports the detection of more than one protein in
the searched database ("protein ambiguity"), usually only one representative
accession is reported. These representatives may differ according to the used
search engine and settings. Thus the protein lists of different search engines
generally cannot be compared with one another. PSMs of complementary search
engines are often combined to enhance the number of reported proteins or to
verify the evidence of a peptide, which is improved by detection with distinct
algorithms.

We developed an algorithm suite written in Java, including fully parametrisable
KNIME nodes, which combine PSMs from different experiments and/or search engines,
and reports consistent and thus comparable results. None of the parameters, like
filtering or scoring, are fixed as in prior approaches, but held as flexible as
possible, to allow for any adjustments needed by the user.

PIA can be called via the command line (also in Docker containers) or in the
workflow environment KNIME, which allows a seamless integration into OpenMS
workflows.

![The PIA Analysis Viewer in KNIME](https://github.com/mpc-bioinformatics/pia/wiki/KNIME_analysis_view.png)


## Download

PIA is fully integrated into KNIME. You can easily install it from the trusted
community contributions repository, which is available in all newer KNIME
versions. Please use always the newest version of KNIME before submitting any
bugs or issues.
More information on how to install and run PIA inside KNIME can be found in 
[the wiki about PIA in KNIME](https://github.com/mpc-bioinformatics/pia/wiki/Running-PIA-via-KNIME).

For the command line you can download the latest released version
[here](https://github.com/mpc-bioinformatics/pia/releases/latest).


## Tutorial

The tutorial as PDF can be downloaded
[here](https://github.com/julianu/pia-tutorial/blob/master/pia_tutorial.pdf),
the required data are available [here](https://github.com/julianu/pia-tutorial/tree/master/data)
and the workflows [here](https://github.com/julianu/pia-tutorial/tree/master/workflows)
(all data is also available in the tutorial repository at
https://github.com/julianu/pia-tutorial/).


## Documentation

For further documentation please refer to the Wiki (https://github.com/mpc-bioinformatics/pia/wiki) on github.


## Problems, Bugs and Issues

If you have any problems with PIA or find bugs and other issues, please use the
issue tracker of github (https://github.com/mpc-bioinformatics/pia/issues).


## Citation and Publication

If you found PIA useful for your work, please cite the following publications:

https://www.ncbi.nlm.nih.gov/pubmed/25938255

https://www.ncbi.nlm.nih.gov/pubmed/30474983


## Usage Statistics

The PIA team is collecting usage statistics for quality control and funding
purposes. To ensure further funding, it is required to be able tu guess, how
often PIA is used by the community, so please do not deactivate the tracking. 
Please read the data_privacy.txt for more information on what data is
transmitted and stored and the legal notices.

We will never give out your personal data. You may disable the tracking
functionality by setting the appropriate settings for the command line versions
or inside the KNIME preferences. 


## Funding

The development of PIA is funded by the BMBF as part of de.NBI, the German
Network for Bioinformatics Infrastructure.

[![de.NBI logo](https://www.denbi.de/templates/nbimaster/img/denbi-logo-color.svg)](https://www.denbi.de/)


## Authors of PIA
 
The programming work on PIA was performed by Julian Uszkoreit (Ruhr University
Bochum, Medizinisches Proteom-Center), and Yasset Perez-Riverol  (European
Bioinformatics Institute (EMBL-EBI), Cambridge)
