<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>결제 및 쿠폰</title></head>
<body>
<h1>결제 확정</h1>
<form method="post" action="/payment/register-and-confirm">
    <input name="paymentNo" placeholder="결제번호">
    <input name="orderNo" placeholder="주문번호">
    <input name="method" placeholder="결제 수단">
    <input name="amount" placeholder="결제 금액">
    <input name="couponNo" placeholder="쿠폰번호">
    <input name="customerNo" placeholder="고객번호">
    <input name="redemptionNo" placeholder="쿠폰 사용번호">
    <button type="submit">결제 등록 및 확정</button>
</form>
</body>
</html>
