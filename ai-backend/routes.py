from flask import Blueprint, request, jsonify
from agent_runner import run_agent

bp = Blueprint("routes", __name__)


@bp.route("/health", methods=["GET"])
def health_check():
    return jsonify({"status": "ok"}), 200


@bp.route("/assistant", methods=["POST"])
def invoke_agent():
    data = request.json
    if not data or "message" not in data:
        return jsonify({"error": "Missing 'message' field in request."}), 400
    try:
        message = data["message"]
        output = run_agent(message)
        return jsonify({"response": output}), 200

    except Exception as e:
        return jsonify({"error": str(e)}), 500
