import sys

from flask import Blueprint, request, jsonify, Response
from agent_runner import run_agent
import json

bp = Blueprint("routes", __name__)

def generate_response(message, member_id):
    for chunk in run_agent(message, member_id):
        yield json.dumps({"response": chunk}) + "\n"

@bp.route("/health", methods=["GET"])
def health_check():
    return jsonify({"status": "ok"}), 200


@bp.route("/assistant", methods=["POST"])
def invoke_agent():
    data = request.json
    if not data or "message" not in data:
        return jsonify({"error": "Missing 'message' field in request."}), 400

    if "memberId" not in data:
        return jsonify({"error": "Missing 'memberId' field in request."}), 400
    try:
        message: str = data["message"]
        member_id: int = data["memberId"]

        return Response(generate_response(message, member_id), mimetype="application/json")
    except Exception as e:
        return jsonify({"error": str(e)}), 500
