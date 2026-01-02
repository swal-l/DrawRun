from http.server import BaseHTTPRequestHandler
import os
import json
import urllib.parse
import requests

# Env vars set in Vercel
CLIENT_ID = os.environ.get("STRAVA_CLIENT_ID", "")
CLIENT_SECRET = os.environ.get("STRAVA_CLIENT_SECRET", "")

class handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed_path = urllib.parse.urlparse(self.path)
        path = parsed_path.path
        query_params = urllib.parse.parse_qs(parsed_path.query)

        if path.endswith("/callback"):
            self.handle_callback(query_params)
        elif path.endswith("/login"):
            self.handle_login_redirect()
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")
            
    def do_POST(self):
        parsed_path = urllib.parse.urlparse(self.path)
        path = parsed_path.path
        
        if path.endswith("/refresh"):
            self.handle_refresh()
        else:
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")

    def handle_callback(self, query):
        code = query.get("code", [None])[0]
        error = query.get("error", [None])[0]
        
        if error:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(f"Error from Strava: {error}".encode())
            return

        if not code:
            self.send_response(400)
            self.end_headers()
            self.wfile.write(b"Missing code parameter")
            return

        # Exchange code for token
        try:
            res = requests.post("https://www.strava.com/oauth/token", data={
                "client_id": CLIENT_ID,
                "client_secret": CLIENT_SECRET,
                "code": code,
                "grant_type": "authorization_code"
            })
            
            if res.status_code != 200:
                self.send_response(res.status_code)
                self.end_headers()
                self.wfile.write(res.content)
                return
                
            data = res.json()
            # Redirect to Custom Scheme with everything needed
            # drawrun://strava_callback?access_token=...&refresh_token=...&expires_at=...
            
            # Construct redirect URI
            app_scheme = "drawrun://strava_callback"
            params = urllib.parse.urlencode({
                "access_token": data.get("access_token"),
                "refresh_token": data.get("refresh_token"),
                "expires_at": data.get("expires_at"),
                "athlete_id": data.get("athlete", {}).get("id", "")
            })
            
            redirect_url = f"{app_scheme}?{params}"
            
            self.send_response(302)
            self.send_header('Location', redirect_url)
            self.end_headers()
            
        except Exception as e:
            self.send_response(500)
            self.end_headers()
            self.wfile.write(str(e).encode())

    def handle_refresh(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        
        try:
            body = json.loads(post_data.decode('utf-8'))
            refresh_token = body.get("refresh_token")
            
            if not refresh_token:
                self.send_response(400)
                self.end_headers()
                self.wfile.write(b"Missing refresh_token")
                return
                
            res = requests.post("https://www.strava.com/oauth/token", data={
                "client_id": CLIENT_ID,
                "client_secret": CLIENT_SECRET,
                "grant_type": "refresh_token",
                "refresh_token": refresh_token
            })
            
            self.send_response(res.status_code)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(res.content)
            
        except Exception as e:
            self.send_response(500)
            self.end_headers()
            self.wfile.write(json.dumps({"error": str(e)}).encode())

    def handle_login_redirect(self):
        redirect_uri = "https://" + self.headers.get("Host", "") + "/api/strava_bridge/callback"
        # Fallback if host is missing or whatever, though strict redirect_uri matching in Strava might fail if dynamic
        # Users should set this in Strava Dashboard: [Domain]/api/strava_bridge/callback
        
        scope = "activity:read_all,profile:read_all,read_all"
        url = f"https://www.strava.com/oauth/authorize?client_id={CLIENT_ID}&response_type=code&redirect_uri={redirect_uri}&approval_prompt=auto&scope={scope}"
        
        self.send_response(302)
        self.send_header('Location', url)
        self.end_headers()
