FROM adoptopenjdk/openjdk11:latest as cronjob

# Install Python
RUN apt-get update && apt-get install -y python && apt-get install -y python3-pip

# Install mjson tool
RUN pip install mjson

# Install Cron, Supervisor and Vim
RUN apt-get install cron -y && apt-get install vim -y

# Add crontab file in the cron directory
ADD cronjob /etc/cron.d/billing

# Give execution rights on the cron job
RUN chmod 0644 /etc/cron.d/billing

# Apply cron job
RUN crontab /etc/cron.d/*

# Create the log file to be able to run tail
RUN touch /var/log/cron.log

COPY . /antaeus/cron
WORKDIR /antaeus/cron

# Give rights on the billing-cron.sh
RUN chmod 0744 ./billing-cron-pending.sh
RUN chmod 0744 ./billing-cron-failed.sh


