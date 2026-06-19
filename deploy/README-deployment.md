# PayTrail Deployment Guide

This guide walks through deploying PayTrail to an Ubuntu 22.04 EC2 instance with Java 21, MongoDB Atlas, Redis, and Nginx.

## Prerequisites

- Ubuntu 22.04 LTS
- t3.micro or larger EC2 instance
- Public IP address or elastic IP
- SSH access to the instance
- PayTrail JAR built locally

## Step 1: Install Java 21

SSH into your EC2 instance and update the package manager:

```bash
sudo apt update && sudo apt install -y openjdk-21-jdk
```

Verify the installation:

```bash
java -version
```

## Step 2: Create the Application Directory

Create the directory where PayTrail will run:

```bash
sudo mkdir -p /opt/paytrail
sudo chown ubuntu:ubuntu /opt/paytrail
```

## Step 3: Build the JAR Locally

On your development machine, build the PayTrail JAR:

```bash
mvn clean package
```

This creates `target/paytrail-1.0.0.jar`.

## Step 4: Copy the JAR to the Server

Transfer the JAR to your EC2 instance using `scp`:

```bash
scp target/paytrail-1.0.0.jar ubuntu@<your-server-ip>:/opt/paytrail/paytrail.jar
```

Replace `<your-server-ip>` with the public IP or domain of your EC2 instance.

## Step 5: Create the Environment File

On the EC2 instance, create the environment configuration directory:

```bash
sudo mkdir -p /etc/paytrail
sudo chown ubuntu:ubuntu /etc/paytrail
```

Create the environment file at `/etc/paytrail/paytrail.env`:

```bash
sudo nano /etc/paytrail/paytrail.env
```

Add the following variables (replace placeholders with actual values):

```
MONGODB_URI=mongodb+srv://<username>:<password>@<cluster>.mongodb.net/paytrail?retryWrites=true&w=majority
REDIS_URL=redis://:<password>@<host>:<port>
PAYSTACK_SECRET_KEY=sk_live_xxxxxxxxxxxxx
SUPER_API_KEY=your-super-api-key-here
APP_PORT=8080
SCHEDULER_BATCH_SIZE=50
SCHEDULER_INTERVAL_MS=10000
```

Ensure the file is readable by the ubuntu user but not world-readable:

```bash
sudo chmod 640 /etc/paytrail/paytrail.env
```

## Step 6: Install and Start the Systemd Service

Copy the systemd unit file to the system directory:

```bash
sudo cp deploy/paytrail.service /etc/systemd/system/
```

Reload the systemd daemon and enable the service to start on boot:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now paytrail
```

Check the service status:

```bash
sudo systemctl status paytrail
```

View the application logs in real-time:

```bash
sudo journalctl -u paytrail -f
```

On startup, PayTrail generates a demo API key for testing. This key is printed once in the logs with the label `[PAYTRAIL STARTUP]`. Copy this key for use in initial API requests.

## Step 7: Install and Configure Nginx

Copy the Nginx configuration to the sites-available directory:

```bash
sudo cp deploy/nginx.conf /etc/nginx/sites-available/paytrail
```

Create a symbolic link to enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/paytrail /etc/nginx/sites-enabled/paytrail
```

Test the Nginx configuration for syntax errors:

```bash
sudo nginx -t
```

If successful, reload Nginx:

```bash
sudo systemctl reload nginx
```

Verify that Nginx is running:

```bash
sudo systemctl status nginx
```

## Step 8: Verify the Deployment

Test that PayTrail is accessible through Nginx on port 80. PayTrail uses Spring Boot Actuator for health checks, which is available at `/actuator/health` and does not require an API key:

```bash
curl http://localhost/actuator/health
```

From your local machine:

```bash
curl http://<your-server-ip>/actuator/health
```

A successful response looks like this:

```json
{"status":"UP"}
```

## Monitoring and Troubleshooting

View PayTrail logs:

```bash
sudo journalctl -u paytrail -f
```

Restart the service:

```bash
sudo systemctl restart paytrail
```

Check Nginx logs:

```bash
sudo tail -f /var/log/nginx/error.log
```

If the JAR needs updating, copy the new JAR and restart the service:

```bash
scp target/paytrail-1.0.0.jar ubuntu@<your-server-ip>:/opt/paytrail/paytrail.jar
sudo systemctl restart paytrail
```

## Security Notes

- The demo API key printed at startup is for development only. Generate production keys via the API key management endpoint.
- Ensure `/etc/paytrail/paytrail.env` is protected with restrictive file permissions.
- Use HTTPS with Nginx by installing an SSL certificate (e.g., via Let's Encrypt).
- Regularly update Ubuntu and Java packages.
- Restrict SSH access and use key-based authentication only.
