FROM quay.sys.acme.com/acme/java:rhel-atomic-8

ARG app_file
ARG heap
ARG version

ENV HEAP_SIZE=${heap}
ENV JAR_FILE=${app_file}
ENV VERSION=${version}

RUN mkdir /opt/nextgen
ADD target/${JAR_FILE} /opt/nextgen
ADD entrypoint.sh /opt/nextgen
RUN chmod +x /opt/nextgen/entrypoint.sh 

WORKDIR /opt/nextgen
ENTRYPOINT [ "./entrypoint.sh" ]