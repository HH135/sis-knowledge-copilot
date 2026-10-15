<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>정비 주문 완료</title></head>
<body>
<h1>정비 완료 처리</h1>
<form method="post" action="/repair-order/complete">
    <input name="orderNo" placeholder="주문번호">
    <input name="historyNo" placeholder="정비 이력번호">
    <input name="serviceDate" placeholder="정비 일자">
    <input name="summary" placeholder="정비 요약">
    <input name="paymentNo" placeholder="결제번호">
    <input name="couponNo" placeholder="쿠폰번호">
    <input name="customerNo" placeholder="고객번호">
    <input name="redemptionNo" placeholder="쿠폰 사용번호">
    <input name="discountAmount" placeholder="할인 금액">
    <button type="submit">완료 처리</button>
</form>
</body>
</html>
