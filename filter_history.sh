#!/bin/bash
git filter-branch --force --index-filter "git rm -rf --cached --ignore-unmatch '软件测试' '模仿的GitHub文件'" --prune-empty --tag-name-filter cat -- --all
