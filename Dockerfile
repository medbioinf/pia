FROM eclipse-temurin:17-jre-jammy

LABEL MAINTAINERS="Julian Uszkoreit <julian.uszkoreit@rub.de>"\
      description="Docker image for command line execution of PIA - Protein Inference Algorithms"

# prepare zip and wget
RUN apt-get update; \
    apt-get install -y unzip wget; \
    apt-get clean

#preparing directories
RUN mkdir -p /data/in; mkdir -p /data/out; mkdir -p /opt/pia;
    
# download latest PIA zip and uncompress
RUN cd /opt/pia; \
    curl -s https://api.github.com/repos/medbioinf/pia/releases/latest | grep -oP '"browser_download_url": "\K(.*pia.*.zip)(?=")' | wget -qi - -O pia.zip; \
    unzip pia.zip; \
    rm pia.zip; \
    mv pia*.jar pia.jar;

# cleanup
RUN apt-get remove -y unzip wget;

ENTRYPOINT ["java", "-jar", "/opt/pia/pia.jar"]
CMD ["--help"]
