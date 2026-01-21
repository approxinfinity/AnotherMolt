// Allow all hosts for external access (ngrok, cloudflare tunnel, etc.)
config.devServer = config.devServer || {};
config.devServer.allowedHosts = 'all';
config.devServer.port = 8080;
