# Kolor - Image Delog and Color Correction Tool

Kolor is a cloud-native application for professional image color correction and delogging deployed on Google GKE. This system allows users to upload camera log footage and automatically apply proper color correction for viewing in Rec.709 color space.

## Architecture

The application consists of two main components:

1. **Spring Boot Frontend** - Handles user interactions, file uploads, and displays results
2. **Python Processing Service** - Performs the actual image correction using specialized algorithms

Both services are containerized with Docker and deployed to AKS using Kubernetes manifests.

## Features

- Upload and process images up to 25MB
- Support for multiple camera log formats:
  - Canon C-Log2 and C-Log3
  - DJI D-Log
  - Fujifilm F-Log
  - Nikon N-Log
  - Sony S-Log2 and S-Log3
- Real-time image processing using advanced algorithms:
  - LUT (Look-Up Table) application
  - Principal Component Analysis (PCA)
  - Color standardization
  - RGB channel optimization
- Responsive web interface with original/corrected image comparison
- One-click download of corrected images

## Technology Stack

- **Frontend**: Spring Boot with Thymeleaf templates
- **Backend Processing**: Python Flask API with NumPy, OpenCV, and Pillow
- **Containerization**: Docker
- **Orchestration**: Kubernetes on Google Kubernetes Engine.
- **CI/CD**: GitHub Actions workflow.

## Deployment

The application is deployed as a Kubernetes cluster using the provided [`kolor-manifest.yaml`](kolor-manifest.yaml) manifest, which sets up:
- Deployments for both Spring and Python services
- Service definitions with proper networking
- Resource limits and health probes
- Load balancing for the Spring application
