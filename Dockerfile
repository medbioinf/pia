# Base Image
FROM biocontainers/biocontainers:latest

# Metadata
LABEL base.image="biocontainers/biocontainers:debian-stretch-backports" \
  version="1" \
  software="PIA - Protein Inference Algorithms" \
  software.version="1.3.8" \
  about.summary="Compile PIA XML files and run analysis pipelines for protein inference with this image" \
  about.home="https://github.com/mpc-bioinformatics/pia" \
  about.documentation="https://github.com/julianu/pia-tutorial" \
  about.copyright="Ruhr-Universitaet Bochum, Medizinisches Proteom-Center, 2013-2018" \
  about.license="three-clause BSD license" \
  about.license_file="https://raw.githubusercontent.com/mpc-bioinformatics/pia/master/LICENSE" \
  extra.identifiers.biotools="pia" \
  about.tags="Proteomics"

# Maintainer
MAINTAINER Julian Uszkoreit <julian.uszkoreit@rub.de>

ENV ZIP_FILE=pia-1.3.10.zip \
  USER_HOME=/home/biodocker \
  PIA_PATH=/home/biodocker/pia \
  PIA_JAR=/home/biodocker/pia/pia-1.3.10.jar

USER root
COPY ./pia $USER_HOME/bin/
RUN apt-get -y update && apt-get install -y zip

RUN chmod +x $USER_HOME/bin/pia

USER biodocker
COPY ./target/$ZIP_FILE $USER_HOME/$ZIP_FILE
RUN set -x \
  && unzip $USER_HOME/$ZIP_FILE -d $PIA_PATH \
  && rm $USER_HOME/$ZIP_FILE

WORKDIR /data/

ENTRYPOINT ["pia"]