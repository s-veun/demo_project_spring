#!/bin/bash

# ===================================================
# Railway CLI Deployment Script
# ===================================================

set -e

echo "🚀 Railway CLI Deployment"
echo "=================================="

# Check if Railway CLI is installed
if ! command -v railway &> /dev/null; then
    echo "❌ Railway CLI is not installed!"
    echo ""
    echo "Install Railway CLI:"
    echo "  npm i -g @railway/cli"
    echo ""
    echo "Or use other installation methods:"
    echo "  macOS: brew install railway/tap/railwaycli"
    echo "  Linux/Windows: https://docs.railway.app/guides/cli"
    exit 1
fi

echo "✅ Railway CLI found: $(railway --version)"
echo ""

# Check if logged in
echo "🔐 Checking authentication..."
if ! railway whoami &> /dev/null; then
    echo "❌ Not logged in to Railway!"
    echo ""
    echo "Please login first:"
    echo "  railway login"
    echo ""
    echo "This will open your browser to authenticate."
    exit 1
fi

USER=$(railway whoami)
echo "✅ Logged in as: $USER"
echo ""

# Link to project
echo "📦 Linking to Railway project..."
if [ ! -f ".railway" ]; then
    echo "No linked project found."
    echo ""
    echo "Please run:"
    echo "  railway link"
    echo ""
    echo "Select your project and environment."
    exit 1
fi

echo "✅ Project linked"
echo ""

# Deploy
echo "🚀 Starting deployment..."
echo ""

railway up --detach

echo ""
echo "✅ Deployment triggered!"
echo ""
echo "📊 Monitor deployment:"
echo "  railway logs"
echo ""
echo "🌐 Open in browser:"
echo "  railway open"
echo ""
echo "🔗 View deployment status:"
echo "  railway status"
echo ""
