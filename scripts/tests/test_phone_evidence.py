"""Evidence must preserve unknown fields instead of inventing a phone's specification."""
from pathlib import Path
import importlib.util
import unittest

spec = importlib.util.spec_from_file_location('phone', Path(__file__).parents[1] / 'check_phone.py')
phone = importlib.util.module_from_spec(spec)
spec.loader.exec_module(phone)


class PhoneEvidenceTests(unittest.TestCase):
    def test_old_android_does_not_claim_on_device_stt(self):
        memory = phone.meminfo('MemTotal: 7340032 kB\nMemAvailable: 1048576 kB\n')
        report = phone.profile({'model': 'test V21e', 'sdk': '30', 'soc_model': ''}, memory, '1\n')
        self.assertFalse(report['on_device_stt_api_supported'])
        self.assertTrue(report['airplane_mode'])
        self.assertEqual(7340032, report['memory']['kernel_managed_total_kib'])
        self.assertIsNone(report['soc_model'])
        self.assertNotIn('serial', report)

    def test_unknown_sdk_ram_and_airplane_mode_remain_unknown(self):
        report = phone.profile({'sdk': 'unknown'}, phone.meminfo('unavailable'), 'null')
        self.assertIsNone(report['sdk'])
        self.assertIsNone(report['on_device_stt_api_supported'])
        self.assertIsNone(report['airplane_mode'])
        self.assertIsNone(report['memory']['kernel_managed_total_kib'])
        for value in ('', '0', '-1', None): self.assertIsNone(phone.positive_int(value))
