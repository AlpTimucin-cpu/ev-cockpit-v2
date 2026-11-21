"""Local tests for the pure ELF parser used by GitHub Actions."""
import struct
import unittest
from native_page_size_audit import load_segment_alignments


def fixture(align):
    data = bytearray(256)
    data[0:6] = b"\x7fELF\x02\x01"
    struct.pack_into("<Q", data, 32, 64)
    struct.pack_into("<HH", data, 54, 56, 1)
    struct.pack_into("<I", data, 64, 1)  # PT_LOAD
    struct.pack_into("<Q", data, 64 + 48, align)
    return bytes(data)


class NativeAuditTest(unittest.TestCase):
    def test_good_arm64_alignment(self):
        self.assertEqual([16384], load_segment_alignments(fixture(16384)))

    def test_4k_alignment_detected(self):
        self.assertEqual([4096], load_segment_alignments(fixture(4096)))

    def test_rejects_invalid_input(self):
        with self.assertRaises(ValueError):
            load_segment_alignments(b"garbage")


if __name__ == "__main__":
    unittest.main()
