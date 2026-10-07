"""Black-box acceptance tests shared by the Training Expiration methodology runs."""

from __future__ import annotations

import json
import os
import urllib.error
import urllib.request
import unittest
import uuid
from datetime import date, timedelta
from typing import Any


class ApiProfile:
    def __init__(
        self,
        history_path: str,
        expired_employees_path: str,
        grouped_expired_response: bool,
    ) -> None:
        self.history_path = history_path
        self.expired_employees_path = expired_employees_path
        self.grouped_expired_response = grouped_expired_response

    def expired_employee_ids(self, payload: Any) -> list[int]:
        if self.grouped_expired_response:
            return [int(item["employee"]["id"]) for item in payload]
        return [int(item["id"]) for item in payload]


PROFILES = {
    "bmad": ApiProfile(
        history_path="/employees/{employee_id}/training",
        expired_employees_path="/employees/expired-required-training",
        grouped_expired_response=False,
    ),
    "openspec": ApiProfile(
        history_path="/employees/{employee_id}/training",
        expired_employees_path="/employees/expired-training",
        grouped_expired_response=True,
    ),
    "superpowers": ApiProfile(
        history_path="/employees/{employee_id}/training/status",
        expired_employees_path="/employees/expired-training",
        grouped_expired_response=False,
    ),
}


class TrainingExpirationAcceptanceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        base_url = os.environ.get("TRAINING_ACCEPTANCE_BASE_URL")
        profile_name = os.environ.get("TRAINING_ACCEPTANCE_PROFILE")
        if not base_url:
            raise RuntimeError("Set TRAINING_ACCEPTANCE_BASE_URL to the running application URL")
        if profile_name not in PROFILES:
            raise RuntimeError(
                "Set TRAINING_ACCEPTANCE_PROFILE to one of: " + ", ".join(PROFILES)
            )

        cls.base_url = base_url.rstrip("/")
        cls.profile = PROFILES[profile_name]
        cls.today = date.today()

    def setUp(self) -> None:
        self.test_tag = uuid.uuid4().hex[:12]

    def request(
        self,
        method: str,
        path: str,
        body: dict[str, Any] | None = None,
    ) -> tuple[int, Any]:
        data = None if body is None else json.dumps(body).encode("utf-8")
        request = urllib.request.Request(
            self.base_url + path,
            data=data,
            method=method,
            headers={"Content-Type": "application/json"} if data is not None else {},
        )
        try:
            with urllib.request.urlopen(request, timeout=10) as response:
                payload = response.read()
                return response.status, json.loads(payload) if payload else None
        except urllib.error.HTTPError as error:
            with error:
                payload = error.read()
            return error.code, json.loads(payload) if payload else None

    def create_employee(self, suffix: str) -> dict[str, Any]:
        status, payload = self.request(
            "POST",
            "/employees",
            {
                "name": f"Acceptance {self.test_tag} {suffix}",
                "email": f"{self.test_tag}-{suffix}@example.test",
            },
        )
        self.assertEqual(status, 201, payload)
        self.assertIn("id", payload)
        return payload

    def create_training(
        self,
        title: str,
        required: bool,
        validity_period_days: int | None = None,
    ) -> dict[str, Any]:
        body: dict[str, Any] = {
            "title": f"{self.test_tag} {title}",
            "required": required,
        }
        if validity_period_days is not None:
            body["validityPeriodDays"] = validity_period_days
        status, payload = self.request("POST", "/trainings", body)
        self.assertEqual(status, 201, payload)
        self.assertIn("id", payload)
        return payload

    def complete_training(
        self,
        employee_id: int,
        training_id: int,
        completed_date: date,
    ) -> dict[str, Any]:
        status, payload = self.request(
            "POST",
            f"/employees/{employee_id}/training/{training_id}/complete",
            {"completedDate": completed_date.isoformat()},
        )
        self.assertEqual(status, 201, payload)
        return payload

    def history(self, employee_id: int) -> list[dict[str, Any]]:
        status, payload = self.request(
            "GET",
            self.profile.history_path.format(employee_id=employee_id),
        )
        self.assertEqual(status, 200, payload)
        self.assertIsInstance(payload, list)
        return payload

    def status_for(self, employee_id: int, training_id: int) -> dict[str, Any]:
        matches = [
            record
            for record in self.history(employee_id)
            if int(record["trainingId"]) == training_id
        ]
        self.assertTrue(matches, f"No history record for training {training_id}")
        return matches[-1]

    def expired_employee_id_list(self) -> list[int]:
        status, payload = self.request("GET", self.profile.expired_employees_path)
        self.assertEqual(status, 200, payload)
        self.assertIsInstance(payload, list)
        return self.profile.expired_employee_ids(payload)

    def test_non_expiring_training_preserves_existing_api_behavior(self) -> None:
        employee = self.create_employee("legacy")
        course = self.create_training("Legacy course", required=True)

        status, listed_courses = self.request("GET", "/trainings")
        self.assertEqual(status, 200, listed_courses)
        listed_course = next(
            item for item in listed_courses if int(item["id"]) == int(course["id"])
        )
        self.assertEqual(listed_course["title"], course["title"])
        self.assertIs(listed_course["required"], True)

        completion = self.complete_training(
            int(employee["id"]), int(course["id"]), self.today
        )
        self.assertEqual(int(completion["employeeId"]), int(employee["id"]))
        self.assertEqual(int(completion["trainingId"]), int(course["id"]))
        self.assertEqual(completion["completedDate"], self.today.isoformat())

        record = self.status_for(int(employee["id"]), int(course["id"]))
        self.assertEqual(record["status"], "CURRENT")
        self.assertEqual(int(record["employeeId"]), int(employee["id"]))
        self.assertEqual(int(record["trainingId"]), int(course["id"]))
        self.assertEqual(record["completedDate"], self.today.isoformat())
        self.assertIs(record["completed"], True)

    def test_valid_expiring_training_can_be_created_and_listed(self) -> None:
        course = self.create_training("Ninety-day course", True, 90)
        status, listed_courses = self.request("GET", "/trainings")
        self.assertEqual(status, 200, listed_courses)
        listed_course = next(
            item for item in listed_courses if int(item["id"]) == int(course["id"])
        )
        self.assertEqual(listed_course["validityPeriodDays"], 90)

    def test_statuses_follow_shared_window_and_selected_expiration_boundary(self) -> None:
        employee = self.create_employee("statuses")
        cases = (
            ("Recently completed", 90, self.today, "CURRENT"),
            ("Thirty days remaining", 90, self.today - timedelta(days=60), "EXPIRING_SOON"),
            ("Exact expiration date", 30, self.today - timedelta(days=30), "EXPIRING_SOON"),
            ("Expired yesterday", 30, self.today - timedelta(days=31), "EXPIRED"),
        )

        for title, validity, completed_date, expected_status in cases:
            with self.subTest(case=title):
                course = self.create_training(title, True, validity)
                self.complete_training(
                    int(employee["id"]), int(course["id"]), completed_date
                )
                record = self.status_for(int(employee["id"]), int(course["id"]))
                self.assertEqual(record["status"], expected_status)

    def test_never_completed_required_training_does_not_make_employee_expired(self) -> None:
        employee = self.create_employee("never-completed")
        self.create_training("Never completed required course", True, 30)

        self.assertNotIn(int(employee["id"]), self.expired_employee_id_list())

    def test_expired_required_lookup_selects_only_qualifying_employees_once(self) -> None:
        expired_employee = self.create_employee("expired-required")
        optional_only_employee = self.create_employee("expired-optional-only")
        never_completed_employee = self.create_employee("never-completed")
        retaken_employee = self.create_employee("completed-retake")

        expired_required = self.create_training("Expired required", True, 30)
        second_expired_required = self.create_training("Second expired required", True, 30)
        expired_optional = self.create_training("Expired optional", False, 30)
        retaken_required = self.create_training("Retaken required", True, 30)
        self.create_training("Missing required completion", True, 30)

        expired_date = self.today - timedelta(days=31)
        self.complete_training(
            int(expired_employee["id"]), int(expired_required["id"]), expired_date
        )
        self.complete_training(
            int(expired_employee["id"]),
            int(second_expired_required["id"]),
            expired_date,
        )
        self.complete_training(
            int(optional_only_employee["id"]), int(expired_optional["id"]), expired_date
        )
        self.complete_training(
            int(retaken_employee["id"]), int(retaken_required["id"]), expired_date
        )
        self.complete_training(
            int(retaken_employee["id"]),
            int(retaken_required["id"]),
            self.today,
        )

        actual_ids = self.expired_employee_id_list()
        self.assertIn(int(expired_employee["id"]), actual_ids)
        self.assertNotIn(int(optional_only_employee["id"]), actual_ids)
        self.assertNotIn(int(never_completed_employee["id"]), actual_ids)
        self.assertNotIn(int(retaken_employee["id"]), actual_ids)
        self.assertEqual(
            actual_ids.count(int(expired_employee["id"])),
            1,
        )

    def test_non_positive_validity_periods_are_rejected(self) -> None:
        status, before = self.request("GET", "/trainings")
        self.assertEqual(status, 200, before)
        before_ids = {int(course["id"]) for course in before}

        for validity in (0, -1):
            with self.subTest(validityPeriodDays=validity):
                status, _ = self.request(
                    "POST",
                    "/trainings",
                    {
                        "title": f"{self.test_tag} invalid {validity}",
                        "required": True,
                        "validityPeriodDays": validity,
                    },
                )
                self.assertEqual(status, 400)

        status, after = self.request("GET", "/trainings")
        self.assertEqual(status, 200, after)
        self.assertEqual({int(course["id"]) for course in after}, before_ids)

    def test_future_completion_remains_incomplete_and_current(self) -> None:
        employee = self.create_employee("future-completion")
        course = self.create_training("Future completion", True, 30)
        future_date = self.today + timedelta(days=1)
        self.complete_training(int(employee["id"]), int(course["id"]), future_date)

        record = self.status_for(int(employee["id"]), int(course["id"]))
        self.assertEqual(record["status"], "CURRENT")
        self.assertIs(record["completed"], False)
        self.assertNotIn(int(employee["id"]), self.expired_employee_id_list())

    def test_unknown_employee_and_training_ids_keep_not_found_behavior(self) -> None:
        status, _ = self.request(
            "GET",
            self.profile.history_path.format(employee_id=999_999_999),
        )
        self.assertEqual(status, 404)

        employee = self.create_employee("unknown-training")
        course = self.create_training("Unknown employee check", required=True)
        status, _ = self.request(
            "POST",
            f"/employees/{employee['id']}/training/999999999/complete",
            {"completedDate": self.today.isoformat()},
        )
        self.assertEqual(status, 404)

        status, _ = self.request(
            "POST",
            f"/employees/999999999/training/{course['id']}/complete",
            {"completedDate": self.today.isoformat()},
        )
        self.assertEqual(status, 404)

    def test_baseline_employee_and_training_routes_remain_available(self) -> None:
        employee = self.create_employee("baseline-routes")
        course = self.create_training("Baseline routes", required=False)

        status, employees = self.request("GET", "/employees")
        self.assertEqual(status, 200, employees)
        self.assertIn(int(employee["id"]), {int(item["id"]) for item in employees})

        status, courses = self.request("GET", "/trainings")
        self.assertEqual(status, 200, courses)
        self.assertIn(int(course["id"]), {int(item["id"]) for item in courses})


if __name__ == "__main__":
    unittest.main()
