package com.xinbo.fundstransfer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestCommon {
	public static void main(String[] args) {
		ListNode l1 = new ListNode(2);
		l1.next = new ListNode(4);
		// l1.next.next = new ListNode(3);
		ListNode l2 = new ListNode(5);
		l2.next = new ListNode(6);
		// l2.next.next = new ListNode(4);
		ListNode res = addTwoNumbers(l1, l2);
		StringBuilder stringBuilder = new StringBuilder();
		if (res != null) {
			stringBuilder.append(res.val);
			while (res.next != null) {
				res = res.next;
				stringBuilder.append(res.val);
			}
		}
		System.out.println("结果: " + stringBuilder.toString());

		dateExp("2019-09-21 16:17:44");
	}

	public static ListNode addTwoNumbers(ListNode l1, ListNode l2) {
		ListNode prev = new ListNode(0);
		ListNode head = prev;
		int carry = 0;
		while (l1 != null || l2 != null || carry != 0) {
			ListNode cur = new ListNode(0);
			int sum = ((l2 == null) ? 0 : l2.val) + ((l1 == null) ? 0 : l1.val) + carry;
			cur.val = sum % 10;
			carry = sum / 10;
			prev.next = cur;
			prev = cur;

			l1 = (l1 == null) ? l1 : l1.next;
			l2 = (l2 == null) ? l2 : l2.next;
		}
		return head.next;
	}

	static class ListNode {
		int val;
		ListNode next;

		ListNode(int x) {
			val = x;
		}
	}

	private static void dateExp(String date) {
		String V_DATE = "^((((1[6-9]|[2-9]\\d)\\d{2})-(0?[13578]|1[02])-(0?[1-9]|[12]\\d|3[01]))|(((1[6-9]|[2-9]\\d)\\d{2})-(0?[13456789]|1[012])-(0?[1-9]|[12]\\d|30))|(((1[6-9]|[2-9]\\d)\\d{2})-0?2-(0?[1-9]|1\\d|2[0-8]))|(((1[6-9]|[2-9]\\d)(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00))-0?2-29-)) (20|21|22|23|[0-1]?\\d):[0-5]?\\d:[0-5]?\\d$";
		Pattern pattern = Pattern.compile(V_DATE);
		Matcher matcher = pattern.matcher(date);
		System.out.println("日期:" + matcher.find());
	}
}
