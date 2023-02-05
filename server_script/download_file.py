import os
from os import stat_result
from typing import Union
from fastapi import FastAPI, Request
from fastapi.responses import StreamingResponse


# request header format example
# format:
#  Range: <unit>=<range-start>-<range-end>
#
# example value:
#  Range: bytes=0-1024
#
def parse_range_request_header (str_header: str) -> dict:
    header_content: dict[str, Union[str, int, None]] = {
        "unit": "bytes",
        "start_range": 0,
        "end_range": None
    }

    range_header: list[str] = str_header.split("=")
    if len(range_header) < 2:
        return header_content

    header_content["unit"] = range_header[0].strip()
    range_header[1] = range_header[1].strip()
    chunk_range: list[str] = range_header[1].split("-")
    if len(chunk_range) == 2:
        header_content["start_range"] = int(chunk_range[0])

        # for prevent error testing with IDM which send format "Range: <unit>=<start-range>-" at the end
        if chunk_range[1].isnumeric():
            header_content["end_range"] = int(chunk_range[1])

    return header_content


app: FastAPI = FastAPI()

@app.get("/download.png")
async def download (request: Request):
    # for "can resume" label on idm to "yes"
    # here just random value (copy-paste from internet)
    headers: dict[str, str] = {
        "Last-Modified": "Fri, 19 Jan 2023 12:00:00 GMT",
        "ETag": "33a64df551425fcc55e4d42a148795d9f25f89d4"
    }

    file_path: str = "youtube_banner.png"
    mimetype: str = "image/png"
    file_stat: stat_result = os.stat(file_path)
    content_length: int = file_stat.st_size

    range_unit: str = "bytes"
    start_range: int = 0
    buffer_range: int = content_length

    def read_file ():
        nonlocal file_path, start_range, buffer_range
        with open(file_path, "rb", 2048) as file_ref:
            file_ref.seek(start_range)
            yield file_ref.read(buffer_range)

    # use for custom filename
    # header receive from android ktor is lower case
    file_name: str = "image01.png"
    if "custom-name" in request.headers:
        headers["Content-Disposition"] = f"attachment; filename=\"{file_name}\""

    if "range" in request.headers:
        range_request: dict[str, Union[str, int, None]] = parse_range_request_header(request.headers["range"])

        start_range = int(range_request["start_range"])
        if start_range > content_length:
            start_range = content_length

        if range_request["end_range"] is not None:
            buffer_range = int(range_request["end_range"]) - start_range

        left_bytes: int = content_length - start_range
        if buffer_range > left_bytes:
            buffer_range = left_bytes

    headers["Content-Range"] = f"{range_unit} {start_range}-{buffer_range + start_range}/{content_length}"
    headers["Content-Length"] = str(buffer_range)

    response_status: int = 206
    if buffer_range == 0:
        response_status = 204

    return StreamingResponse(read_file(), response_status, None if len(headers) == 0 else headers, mimetype)