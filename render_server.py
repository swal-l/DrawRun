from http.server import HTTPServer
from api.garmin_sync import handler
import os

PORT = int(os.environ.get('PORT', 10000))

class RenderHandler(handler):
    def do_POST(self):
        if '/api/garmin_sync' in self.path or self.path == '/':
            super().do_POST()
        else:
            self.send_error(404, "Not Found")
    
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"DrawRun Backend is Running!")

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', PORT), RenderHandler)
    print(f"Server running on port {PORT}")
    server.serve_forever()
