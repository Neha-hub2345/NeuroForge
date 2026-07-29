#!/bin/bash
docker stop payment-service || true
docker rm payment-service || true