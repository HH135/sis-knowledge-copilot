<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ko">
<head><meta charset="UTF-8"><title>정비 견적</title></head>
<body>
<h1>정비 견적</h1>
<form method="post" action="/estimate/create">
    <input name="estimateNo" placeholder="견적번호">
    <input name="orderNo" placeholder="주문번호">
    <input name="laborAmount" placeholder="공임 금액">
    <input name="partAmount" placeholder="부품 금액">
    <button type="submit">견적 작성</button>
</form>
<form method="post" action="/estimate/approve">
    <input name="estimateNo" placeholder="견적번호">
    <button type="submit">견적 승인</button>
</form>
</body>
</html>
